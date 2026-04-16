// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

extra["dexOutputName"] = "instagram-settings.dex"
extra["dexExcludeClasses"] = "com/instagram/base/activity/IgActivity.class"

apply(from = rootDir.resolve("android-extension-module.gradle.kts"))

dependencies {
    add("compileOnly", project(":shared-settings-runtime"))
}
