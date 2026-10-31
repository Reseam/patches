// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.pinterest.settings

import app.reseam.patch.settings.SettingsGroup
import app.reseam.patch.settings.folder
import app.reseam.patch.settings.toggle
import app.reseam.patches.bundle.lucide

object FeedSettings : SettingsGroup(PinterestStrings) {
    val hideAds by toggle(default = true, icon = lucide("megaphone-off"))
    val hideShopping by toggle(default = false, icon = lucide("shopping-bag"))
}

object UpdateSettings : SettingsGroup(PinterestStrings) {
    val disableUpdatePrompts by toggle(default = true, icon = lucide("shield-check"))
}

object ShareSettings : SettingsGroup(PinterestStrings) {
    val hideScreenshotShare by toggle(default = true, icon = lucide("camera"))
}

object ImageSettings : SettingsGroup(PinterestStrings) {
    val originalQuality by toggle(default = true, icon = lucide("image"))
}

object DownloadSettings : SettingsGroup(PinterestStrings) {
    val downloadFolder by folder(default = "Pinterest", icon = lucide("folder-open"))
}

object CloseupSettings : SettingsGroup(PinterestStrings) {
    val showPostedDate by toggle(default = true, icon = lucide("calendar"))
}

object PromptSettings : SettingsGroup(PinterestStrings) {
    val hideSurveys by toggle(default = true, icon = lucide("message-circle-off"))
}
