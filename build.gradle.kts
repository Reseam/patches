// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

val patchProjects = subprojects.filter { it.path.endsWith(":patch") }
val dexProjects = subprojects.filter {
    it.path.contains(":extensions:") || it.name == "shared-settings-runtime"
}

val stageBundle = tasks.register<Copy>("stageBundle") {
    group = "build"
    description = "Stages manifest + patch JARs + extension DEXes for packaging."

    dependsOn(patchProjects.map { "${it.path}:jar" })
    dependsOn(dexProjects.map { "${it.path}:build" })

    val outDir = layout.buildDirectory.dir("bundle-stage")
    into(outDir)

    doFirst { outDir.get().asFile.deleteRecursively() }

    from("manifest.toml")

    patchProjects.forEach { proj ->
        val appName = proj.path.removePrefix(":apps:").substringBefore(":")
        from(proj.tasks.named("jar").flatMap { (it as Jar).archiveFile }) {
            rename { "$appName-patches.jar" }
        }
    }

    from(dexProjects.map { it.layout.buildDirectory.dir("dex") }) {
        include("*.dex")
    }
}

tasks.register<Exec>("bundle") {
    group = "build"
    description = "Packs a signed reseam-patches.reseam bundle."
    dependsOn(stageBundle)

    val stageDir = layout.buildDirectory.dir("bundle-stage")
    val outFile = layout.buildDirectory.file("bundle/reseam-patches.reseam")
    val reseamBin = providers.environmentVariable("RESEAM_BIN")
        .orElse("${projectDir}/../reseam/target/release/reseam")
    val signingKey = providers.environmentVariable("RESEAM_BUNDLE_KEY")
        .orElse("${System.getProperty("user.home")}/.reseam/bundle-signing.key")

    doFirst {
        outFile.get().asFile.parentFile.mkdirs()
        if (outFile.get().asFile.exists()) outFile.get().asFile.delete()
    }

    commandLine = listOf(
        reseamBin.get(),
        "bundle", "pack",
        stageDir.get().asFile.absolutePath,
        "--key", signingKey.get(),
        "--out", outFile.get().asFile.absolutePath,
    )
}

tasks.register<Exec>("generatePatchesJson") {
    group = "distribution"
    description = "Generates patches.json for the signed official patch bundle."
    dependsOn("bundle")

    val bundleFile = layout.buildDirectory.file("bundle/reseam-patches.reseam")
    val outFile = providers.environmentVariable("RESEAM_PATCHES_JSON_OUT")
        .map { file(it) }
        .orElse(layout.buildDirectory.file("bundle/patches.json").map { it.asFile })
    val reseamBin = providers.environmentVariable("RESEAM_BIN")
        .orElse("${projectDir}/../reseam/target/release/reseam")
    val version = providers.environmentVariable("RESEAM_RELEASE_VERSION")
    val bundleUrl = providers.environmentVariable("RESEAM_BUNDLE_URL")
    val description = providers.environmentVariable("RESEAM_RELEASE_DESCRIPTION")
    val descriptionFile = providers.environmentVariable("RESEAM_RELEASE_DESCRIPTION_FILE")
    val homepage = providers.environmentVariable("RESEAM_HOMEPAGE")
        .orElse("https://reseam.app")
    val createdAt = providers.environmentVariable("RESEAM_RELEASE_CREATED_AT")
    val prerelease = providers.environmentVariable("RESEAM_RELEASE_PRERELEASE")
        .map { it.equals("true", ignoreCase = true) || it == "1" }
        .orElse(false)

    inputs.file(bundleFile)
    inputs.property("version", version)
    inputs.property("bundleUrl", bundleUrl)
    inputs.property("description", description.orElse(""))
    inputs.property("descriptionFile", descriptionFile.orElse(""))
    inputs.property("homepage", homepage)
    inputs.property("createdAt", createdAt.orElse(""))
    inputs.property("prerelease", prerelease.map { it.toString() })
    outputs.file(outFile)

    doFirst {
        val args = mutableListOf(
            reseamBin.get(),
            "publish", "patches",
            bundleFile.get().asFile.absolutePath,
            "--version", version.orNull
                ?: error("RESEAM_RELEASE_VERSION is required to generate patches.json"),
            "--url", bundleUrl.orNull
                ?: error("RESEAM_BUNDLE_URL is required to generate patches.json"),
            "--homepage", homepage.get(),
            "--out", outFile.get().absolutePath,
        )

        description.orNull?.let { args += listOf("--description", it) }
        descriptionFile.orNull?.let { args += listOf("--description-file", file(it).absolutePath) }
        createdAt.orNull?.let { args += listOf("--created-at", it) }
        if (prerelease.get()) args += "--prerelease"

        outFile.get().parentFile.mkdirs()
        commandLine = args
    }
}
