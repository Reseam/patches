// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.twitch.settings

import app.reseam.patch.settings.SettingsGroup
import app.reseam.patch.settings.text
import app.reseam.patch.settings.toggle
import app.reseam.patches.bundle.lucide

object AdsSettings : SettingsGroup(TwitchStrings) {
    val blockVideoAds by toggle(default = true, icon = lucide("megaphone-off"))
    val blockEmbeddedAds by toggle(default = false, icon = lucide("monitor-off"))
    val embeddedAdsProxy by text(default = "https://eu.luminous.dev", icon = lucide("server"))
    val ignoreAdEvents by toggle(default = true, icon = lucide("bell-off"))
}

object PrivacySettings : SettingsGroup(TwitchStrings) {
    val blockTrackers by toggle(default = true, icon = lucide("shield"))
}

object UpdateSettings : SettingsGroup(TwitchStrings) {
    val disableUpdatePrompt by toggle(default = true, icon = lucide("refresh-cw-off"))
}

object DeveloperSettings : SettingsGroup(TwitchStrings) {
    val debugMode by toggle(default = false, icon = lucide("bug"))
}
