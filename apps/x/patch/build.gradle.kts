// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

reseamStrings { objectName = "app.reseam.patches.x.XPatchStrings" }

dependencies {
    compileOnly(project(":shared:bundle:patch"))
    compileOnly(project(":shared:fonts:patch"))
    compileOnly(project(":shared:ai-labels:patch"))
    compileOnly(project(":shared:pangram:patch"))
}
