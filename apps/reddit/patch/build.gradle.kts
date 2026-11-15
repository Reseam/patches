// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

reseamStrings { objectName = "app.reseam.patches.reddit.RedditPatchStrings" }

dependencies {
    compileOnly(project(":shared:bundle:patch"))
    compileOnly(project(":apps:universal:patch"))
    compileOnly(project(":shared:ai-labels:patch"))
    compileOnly(project(":shared:pangram:patch"))
}
