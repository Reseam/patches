// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

val patchProjects = subprojects.filter { it.path.endsWith(":patch") }
val dexProjects = subprojects.filter {
    it.path.contains(":extensions:") || it.name == "shared-settings-runtime"
}

val reseamWorkspace: String? = (providers.gradleProperty("reseam.workspace").orNull
    ?: System.getenv("RESEAM_WORKSPACE"))?.takeIf { it.isNotBlank() }

val reseamBin: Provider<String> = providers.environmentVariable("RESEAM_BIN")
    .orElse(providers.provider {
        reseamWorkspace?.let { "$it/target/release/reseam" }
            ?: throw GradleException(
                "RESEAM_BIN env var or -Preseam.workspace property required to locate the reseam CLI"
            )
    })

fun d8Executable(): String {
    System.getenv("D8_BIN")?.takeIf { it.isNotBlank() }?.let { return it }
    val androidHome = System.getenv("ANDROID_HOME")
        ?: System.getenv("ANDROID_SDK_ROOT")
        ?: error("ANDROID_HOME, ANDROID_SDK_ROOT, or D8_BIN is required to build Android patch jars")
    val buildTools = file("$androidHome/build-tools")
        .listFiles()
        ?.filter { file("${it.absolutePath}/d8").isFile }
        ?.maxByOrNull { it.name }
        ?: error("Could not find d8 under $androidHome/build-tools")
    return file("${buildTools.absolutePath}/d8").absolutePath
}

val universalPatchJarFiles = patchProjects.map { proj ->
    val appName = proj.path.removePrefix(":apps:").substringBefore(":")
    val inputJar = providers.provider {
        (proj.tasks.named("jar").get() as Jar).archiveFile.get().asFile
    }
    val dexJar = layout.buildDirectory.file("patch-dex-jars/$appName-patches-dex.jar")
    val dexTaskName = "${proj.path.replace(':', '_').trim('_')}PatchDexJar"
    val dexTask = tasks.register<Exec>(dexTaskName) {
        group = "build"
        description = "Dexes ${proj.path} so its patch JAR can load on Android."
        dependsOn("${proj.path}:jar")

        inputs.file(inputJar)
        outputs.file(dexJar)

        doFirst {
            val outFile = dexJar.get().asFile
            outFile.parentFile.mkdirs()
            if (outFile.exists()) outFile.delete()
            commandLine(
                d8Executable(),
                "--release",
                "--min-api", "26",
                "--output", outFile.absolutePath,
                inputJar.get().absolutePath,
            )
        }
    }

    val universalTaskName = "${proj.path.replace(':', '_').trim('_')}UniversalPatchJar"
    val universalTask = tasks.register<Jar>(universalTaskName) {
        group = "build"
        description = "Builds a universal JVM/Android patch JAR for ${proj.path}."
        dependsOn(dexTask)

        archiveFileName.set("$appName-patches.jar")
        destinationDirectory.set(layout.buildDirectory.dir("universal-patch-jars"))

        from(inputJar.map { zipTree(it) })
        from(dexJar.map { zipTree(it) }) {
            include("classes*.dex")
        }
    }

    universalTask.flatMap { it.archiveFile }.map { it.asFile }
}

val stageBundle = tasks.register<Copy>("stageBundle") {
    group = "build"
    description = "Stages manifest + universal patch JARs + extension DEXes for packaging."

    dependsOn(universalPatchJarFiles)
    dependsOn(dexProjects.map { "${it.path}:build" })

    val outDir = layout.buildDirectory.dir("bundle-stage")
    into(outDir)

    doFirst { outDir.get().asFile.deleteRecursively() }

    from("manifest.toml")

    from(universalPatchJarFiles)

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
    val releaseTag = providers.gradleProperty("releaseTag")
    val version = providers.environmentVariable("RESEAM_RELEASE_VERSION")
        .orElse(releaseTag.map { it.removePrefix("v") })
    val bundleUrl = providers.environmentVariable("RESEAM_BUNDLE_URL")
        .orElse(releaseTag.map { "https://api.reseam.app/patches/$it/reseam-patches.reseam" })
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

tasks.register<Sync>("stageRelease") {
    group = "distribution"
    description = "Collects the bundle and patches.json for a release."
    dependsOn("generatePatchesJson")
    from(layout.buildDirectory.dir("bundle")) { include("reseam-patches.reseam", "patches.json") }
    into(layout.buildDirectory.dir("release"))
}
