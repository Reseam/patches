import org.gradle.api.plugins.JavaPluginExtension

apply(plugin = "java-library")

val dexOutputName = extra["dexOutputName"] as String
val dexExcludeClasses = (extra.properties["dexExcludeClasses"] as? String)
    ?.split(",")
    ?.map { it.trim() }
    ?.filter { it.isNotEmpty() }
    .orEmpty()

val androidHome: String = System.getenv("ANDROID_HOME")
    ?: error("ANDROID_HOME is not set; required to locate android.jar and d8")

val androidJar: File = file("$androidHome/platforms").listFiles().orEmpty()
    .filter { it.name.startsWith("android-") && File(it, "android.jar").exists() }
    .maxByOrNull { it.name.removePrefix("android-").toIntOrNull() ?: -1 }
    ?.resolve("android.jar")
    ?: error("No platforms/android-*/android.jar found under $androidHome")

val d8Binary: File = file("$androidHome/build-tools").listFiles().orEmpty()
    .filter { File(it, "d8").exists() }
    .maxByOrNull { it.name }
    ?.resolve("d8")
    ?: error("No build-tools/*/d8 found under $androidHome")

configure<JavaPluginExtension> {
    toolchain { languageVersion.set(JavaLanguageVersion.of(17)) }
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    "compileOnly"(files(androidJar))
}

val dex = tasks.register<Exec>("dex") {
    dependsOn("classes")
    group = "build"
    description = "d8 -> $dexOutputName"

    val classesRoot = layout.buildDirectory.dir("classes/java/main").get().asFile
    val stageDir = layout.buildDirectory.dir("dex-stage").get().asFile
    val finalDex = layout.buildDirectory.file("dex/$dexOutputName").get().asFile

    inputs.dir(classesRoot)
    outputs.file(finalDex)

    doFirst {
        stageDir.deleteRecursively()
        stageDir.mkdirs()
        finalDex.parentFile.mkdirs()
    }

    commandLine = mutableListOf(
        d8Binary.absolutePath,
        "--min-api", "28",
        "--output", stageDir.absolutePath,
    )
    doFirst {
        val classFiles = fileTree(classesRoot) {
            include("**/*.class")
            dexExcludeClasses.forEach { exclude(it) }
        }.files
        check(classFiles.isNotEmpty()) { "No compiled classes under $classesRoot" }
        commandLine = (commandLine + classFiles.map { it.absolutePath }).toList()
    }

    doLast {
        val staged = File(stageDir, "classes.dex")
        check(staged.exists()) { "d8 did not produce $staged" }
        finalDex.delete()
        staged.renameTo(finalDex)
    }
}

tasks.named("build") { dependsOn(dex) }
