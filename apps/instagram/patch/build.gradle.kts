plugins {
    kotlin("jvm") version "1.9.25"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(
        files(
            fileTree("${rootDir}/../stitch/kotlin-sdk/build/libs") {
                include("stitch-patch-sdk-*.jar")
            }
        )
    )
}

kotlin {
    jvmToolchain(17)
}

tasks.jar {
    archiveBaseName.set("instagram-patches")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(
        configurations.runtimeClasspath.get()
            .filter { it.name.endsWith(".jar") }
            .map { zipTree(it) }
    )
}
