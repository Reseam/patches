// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.pinterest.settings

import app.reseam.patch.settings.SettingsGroup
import app.reseam.patch.settings.TakesEffect
import app.reseam.patch.settings.Vector
import app.reseam.patch.settings.folder
import app.reseam.patch.settings.toggle

object FeedSettings : SettingsGroup(PinterestStrings) {
    val hideAds by toggle(default = true, icon = Vector.lucide("megaphone-off"))
    val hideShopping by toggle(default = false, icon = Vector.lucide("shopping-bag"))
}

object UpdateSettings : SettingsGroup(PinterestStrings) {
    val disableUpdatePrompts by toggle(default = true, icon = Vector.lucide("shield-check"))
}

object ShareSettings : SettingsGroup(PinterestStrings) {
    val hideScreenshotShare by toggle(default = true, icon = Vector.lucide("camera"))
}

object ImageSettings : SettingsGroup(PinterestStrings) {
    val originalQuality by toggle(default = true, icon = Vector.lucide("image"))
}

object DownloadSettings : SettingsGroup(PinterestStrings) {
    val downloadFolder by
        folder(
            default = "Pinterest",
            icon = Vector.lucide("folder-open"),
            takesEffect = TakesEffect.IMMEDIATELY,
        )
    val askWhichMedia by
        toggle(
            default = true,
            icon = Vector.lucide("images"),
            takesEffect = TakesEffect.IMMEDIATELY,
        )
}

object CloseupSettings : SettingsGroup(PinterestStrings) {
    val showPostedDate by toggle(default = true, icon = Vector.lucide("calendar"))
}

object PromptSettings : SettingsGroup(PinterestStrings) {
    val hideSurveys by toggle(default = true, icon = Vector.lucide("message-circle-off"))
}
