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
    val embeddedAdsProxy by
        choice(
            default = "https://eu.luminous.dev",
            choices =
                listOf(
                    Choice("https://eu.luminous.dev", TwitchStrings.embeddedAdsProxyEuLuminous),
                    Choice("https://eu2.luminous.dev", TwitchStrings.embeddedAdsProxyEu2Luminous),
                    Choice("https://eu3.luminous.dev", TwitchStrings.embeddedAdsProxyEu3Luminous),
                    Choice("https://as.luminous.dev", TwitchStrings.embeddedAdsProxyAsLuminous),
                    Choice("https://lb-eu.cdn-perfprod.com", TwitchStrings.embeddedAdsProxyEuPerfprod),
                    Choice("https://lb-eu2.cdn-perfprod.com", TwitchStrings.embeddedAdsProxyEu2Perfprod),
                    Choice("https://lb-eu3.cdn-perfprod.com", TwitchStrings.embeddedAdsProxyEu3Perfprod),
                    Choice("https://lb-eu4.cdn-perfprod.com", TwitchStrings.embeddedAdsProxyEu4Perfprod),
                    Choice("https://lb-eu5.cdn-perfprod.com", TwitchStrings.embeddedAdsProxyEu5Perfprod),
                    Choice("https://lb-na.cdn-perfprod.com", TwitchStrings.embeddedAdsProxyNaPerfprod),
                    Choice("https://lb-sa.cdn-perfprod.com", TwitchStrings.embeddedAdsProxySaPerfprod),
                    Choice("https://lb-as.cdn-perfprod.com", TwitchStrings.embeddedAdsProxyAsPerfprod),
                    Choice("https://twitch.nadeko.net", TwitchStrings.embeddedAdsProxyNadeko),
                    Choice("https://twitch-al.nadeko.net", TwitchStrings.embeddedAdsProxyAlNadeko),
                    Choice("custom", TwitchStrings.embeddedAdsProxyCustom),
                ),
            icon = Vector.lucide("server"),
        )
    val customEmbeddedAdsProxy by text(default = "", icon = Vector.lucide("link"))
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
