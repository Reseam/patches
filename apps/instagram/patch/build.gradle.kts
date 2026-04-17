// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

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
            fileTree("${rootDir}/../reseam/kotlin-sdk/build/libs") {
                include("reseam-patch-sdk-*.jar")
            }
        )
    )
}

kotlin {
    jvmToolchain(21)
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
