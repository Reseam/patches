// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

plugins {
    kotlin("jvm") version "1.9.25"
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://git.reseam.app/api/packages/reseam/maven")
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("app.reseam:reseam-patch-sdk:0.1.0")
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
