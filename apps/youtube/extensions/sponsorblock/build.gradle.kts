// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

dependencies {
    compileOnly(project(":shared:settings-runtime"))
    compileOnly(project(":apps:youtube:extensions:core"))
    compileOnly(project(":apps:youtube:extensions:player"))
    compileOnly(project(":apps:youtube:extensions:video"))
    compileOnly(project(":apps:youtube:extensions:controls"))
}

// Exercise the interval math without Android stubs or a live SponsorBlock service.
val compileTimelineRegression by tasks.registering(JavaCompile::class) {
    source("src/main/java/app/reseam/youtube/sponsorblock/SegmentTimeline.java",
        "src/test/java/app/reseam/youtube/sponsorblock/SegmentTimelineTest.java")
    classpath = files()
    destinationDirectory.set(layout.buildDirectory.dir("timeline-regression"))
    options.release.set(17)
}
val testTimeline by tasks.registering(JavaExec::class) {
    dependsOn(compileTimelineRegression)
    classpath = files(compileTimelineRegression.flatMap { it.destinationDirectory })
    mainClass.set("app.reseam.youtube.sponsorblock.SegmentTimelineTest")
}
tasks.matching { it.name == "check" }.configureEach { dependsOn(testTimeline) }
