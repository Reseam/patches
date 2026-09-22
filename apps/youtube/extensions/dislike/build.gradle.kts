// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

dependencies {
    compileOnly(project(":shared:settings-runtime"))
    compileOnly(project(":apps:youtube:extensions:core"))
    compileOnly(project(":apps:youtube:extensions:player"))
    compileOnly(project(":apps:youtube:extensions:video"))
    compileOnly(project(":apps:youtube:extensions:litho"))
}

// Runs against Android's real Spannable/ICU implementations in a separate app_process.
// HTTP is intercepted inside that process; no public votes or app preferences are touched.
val mainSources = extensions.getByType<SourceSetContainer>().named("main")
val compileDeviceRegression by tasks.registering(JavaCompile::class) {
    dependsOn(tasks.named("classes"))
    source("src/test/java")
    classpath = mainSources.get().compileClasspath + mainSources.get().output
    destinationDirectory.set(layout.buildDirectory.dir("device-regression/classes"))
    options.release.set(17)
}
val deviceRegressionJar by tasks.registering(Jar::class) {
    dependsOn(compileDeviceRegression)
    from(compileDeviceRegression.flatMap { it.destinationDirectory })
    archiveFileName.set("ryd-device-regression.jar")
    destinationDirectory.set(layout.buildDirectory.dir("device-regression"))
}
