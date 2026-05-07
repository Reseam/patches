// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

extra["dexOutputName"] = "telegram-settings.dex"
extra["dexExcludeClasses"] = listOf(
    "org/telegram/ui/ActionBar/BaseFragment.class",
    "org/telegram/ui/Cells/TextCheckCell.class",
    "org/telegram/ui/Cells/HeaderCell.class",
    "org/telegram/ui/Cells/TextCell.class",
    "org/telegram/ui/Components/UItem.class",
    "org/telegram/ui/Components/UniversalAdapter.class",
).joinToString(",")

apply(from = rootDir.resolve("android-extension-module.gradle.kts"))

dependencies {
    add("compileOnly", project(":shared-settings-runtime"))
}
