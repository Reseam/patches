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
    val blockEmbeddedAds by toggle(default = false, icon = Vector.lucide("monitor-off"))
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

object ChatSettings : SettingsGroup(TwitchStrings) {
    val autoClaimRewards by toggle(default = true, icon = Vector.lucide("gift"))
    val hideLeaderboards by toggle(default = false, icon = Vector.lucide("trophy"))
}

object LinkSettings : SettingsGroup(TwitchStrings) {
    val cleanShareLinks by toggle(default = true, icon = Vector.lucide("link-2-off"))
}

private val videoQualities =
    listOf(
        Choice("source", TwitchStrings.videoQualitySource),
        Choice("1080", TwitchStrings.videoQuality1080),
        Choice("720", TwitchStrings.videoQuality720),
        Choice("480", TwitchStrings.videoQuality480),
        Choice("360", TwitchStrings.videoQuality360),
        Choice("160", TwitchStrings.videoQuality160),
        Choice("auto", TwitchStrings.videoQualityAuto),
    )

object PlaybackSettings : SettingsGroup(TwitchStrings) {
    val defaultVideoQuality by choice(default = "source", choices = videoQualities, icon = Vector.lucide("monitor-play"))
    val mobileDataQuality by
        choice(
            default = "same",
            choices = listOf(Choice("same", TwitchStrings.videoQualitySameAsWifi)) + videoQualities,
            icon = Vector.lucide("signal"),
        )
    val rememberVideoQuality by toggle(default = true, icon = Vector.lucide("bookmark-check"))
    val refreshStream by toggle(default = true, icon = Vector.lucide("refresh-cw"))
}

object PlayerOverlaySettings : SettingsGroup(TwitchStrings) {
    val hideClipButton by toggle(default = false, icon = Vector.lucide("clapperboard"))
    val hideCastButton by toggle(default = false, icon = Vector.lucide("cast"))
    val hideShareButton by toggle(default = false, icon = Vector.lucide("share-2"))
}

object SubscriptionSettings : SettingsGroup(TwitchStrings) {
    val hideSubscriptionUpsells by toggle(default = true, icon = Vector.lucide("star-off"))
}

object DeveloperSettings : SettingsGroup(TwitchStrings) {
    val debugMode by toggle(default = true, icon = Vector.lucide("bug"))
}
