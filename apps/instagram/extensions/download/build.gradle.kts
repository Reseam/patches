// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

extra["dexOutputName"] = "instagram-download.dex"

apply(from = rootDir.resolve("android-extension-module.gradle.kts"))

dependencies {
    add("compileOnly", project(":shared-settings-runtime"))
    add("compileOnly", project(":apps:instagram:extensions:refs"))
}
