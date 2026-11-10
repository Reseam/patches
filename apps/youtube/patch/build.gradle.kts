// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

reseamStrings { objectName = "app.reseam.patches.youtube.YouTubePatchStrings" }

dependencies {
    compileOnly(project(":shared:bundle:patch"))
    compileOnly(project(":shared:gmscore:patch"))
    compileOnly(project(":shared:youtube-common:patch"))
    compileOnly(project(":shared:youtube-save-to-device:patch"))
    compileOnly(project(":shared:youtube-spoof:patch"))
}
