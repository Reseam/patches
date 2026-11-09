// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.twitch.settings

import app.reseam.patch.settings.Choice
import app.reseam.patch.settings.SettingsGroup
import app.reseam.patch.settings.Vector
import app.reseam.patch.settings.choice
import app.reseam.patch.settings.text
import app.reseam.patch.settings.toggle

object AdsSettings : SettingsGroup(TwitchStrings) {
    val blockVideoAds by toggle(default = true, icon = Vector.lucide("megaphone-off"))
    val blockEmbeddedAds by toggle(default = true, icon = Vector.lucide("monitor-off"))
    val embeddedAdsProxy by text(default = "https://eu.luminous.dev", icon = Vector.lucide("server"))
    val ignoreAdEvents by toggle(default = true, icon = Vector.lucide("bell-off"))
}

object PrivacySettings : SettingsGroup(TwitchStrings) {
    val blockAnalytics by toggle(default = true, icon = Vector.lucide("shield"))
}

object UpdateSettings : SettingsGroup(TwitchStrings) {
    val disableUpdatePrompts by toggle(default = true, icon = Vector.lucide("shield-check"))
}

object NavigationSettings : SettingsGroup(TwitchStrings) {
    val startupTab by
        choice(
            default = "home",
            choices =
                listOf(
                    Choice("home", TwitchStrings.startupTabHome),
                    Choice("browse", TwitchStrings.startupTabBrowse),
                    Choice("profile", TwitchStrings.startupTabProfile),
                ),
            icon = Vector.lucide("house"),
        )
}

object DeveloperSettings : SettingsGroup(TwitchStrings) {
    val debugMode by toggle(default = true, icon = Vector.lucide("bug"))
}
