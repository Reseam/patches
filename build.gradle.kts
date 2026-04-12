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
    description = "Packs a signed stitch-patches.stitch bundle."
    dependsOn(stageBundle)

    val stageDir = layout.buildDirectory.dir("bundle-stage")
    val outFile = layout.buildDirectory.file("bundle/stitch-patches.stitch")
    val stitchBin = providers.environmentVariable("STITCH_BIN")
        .orElse("${projectDir}/../stitch/target/release/stitch")
    val signingKey = providers.environmentVariable("STITCH_BUNDLE_KEY")
        .orElse("${System.getProperty("user.home")}/.stitch/bundle-signing.key")

    doFirst {
        outFile.get().asFile.parentFile.mkdirs()
        if (outFile.get().asFile.exists()) outFile.get().asFile.delete()
    }

    commandLine = listOf(
        stitchBin.get(),
        "bundle", "pack",
        stageDir.get().asFile.absolutePath,
        "--key", signingKey.get(),
        "--out", outFile.get().asFile.absolutePath,
    )
}
