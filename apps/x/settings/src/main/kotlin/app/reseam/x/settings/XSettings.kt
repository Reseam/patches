// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.x.settings

import app.reseam.patch.settings.Choice
import app.reseam.patch.settings.SettingsGroup
import app.reseam.patch.settings.Vector
import app.reseam.patch.settings.action
import app.reseam.patch.settings.choice
import app.reseam.patch.settings.folder
import app.reseam.patch.settings.text
import app.reseam.patch.settings.toggle

object XSettings : SettingsGroup(XStrings) {
    val defaultToFollowing by toggle(default = false, icon = Vector.lucide("users"))
    val disableHomeTabSwipe by toggle(default = false, icon = Vector.lucide("move-horizontal"))
    val hidePromotionPrompts by toggle(default = true, icon = Vector.lucide("rocket"))
    val hideAds by toggle(default = true, icon = Vector.lucide("megaphone-off"))
    val hidePremiumUpsells by toggle(default = true, icon = Vector.lucide("gem"))
    val hideAppOpenPaywall by toggle(default = true, icon = Vector.lucide("lock"))
    val unlockPremium by toggle(default = true, icon = Vector.lucide("badge-check"))
    val downloadAnyVideo by toggle(default = true, icon = Vector.lucide("download"))
    val downloadWithoutWatermark by toggle(default = true, icon = Vector.lucide("droplet-off"))
    val downloadFolder by folder(default = "X", icon = Vector.lucide("folder-open"))
    val saveAllMedia by toggle(default = true, icon = Vector.lucide("images"))
    val downloadButton by toggle(default = true, icon = Vector.lucide("download"))
    val showSensitiveMedia by toggle(default = false, icon = Vector.lucide("eye"))

    val hideGrokTab by toggle(default = true, icon = Vector.lucide("bot-off"))
    val hideGrokPostButton by toggle(default = true, icon = Vector.lucide("sparkles"))
    val hideGrokDrawerEntry by toggle(default = true, icon = Vector.lucide("panel-left"))
    val hideGrokPrompts by toggle(default = true, icon = Vector.lucide("message-square-dashed"))
    val hideGrokImageTools by toggle(default = true, icon = Vector.lucide("image-off"))
    val hideGrokComposer by toggle(default = true, icon = Vector.lucide("pen-off"))
    val hideLiveBar by toggle(default = true, icon = Vector.lucide("radio"))
    val hideLiveAvatarRings by toggle(default = false, icon = Vector.lucide("circle-dot"))
    val hideRecommendations by toggle(default = false, icon = Vector.lucide("user-plus"))
    val showWithheldPosts by toggle(default = false, icon = Vector.lucide("eye"))
    val hideViewCounts by toggle(default = false, icon = Vector.lucide("chart-no-axes-column"))
    val hideNewPostsBanner by toggle(default = false, icon = Vector.lucide("arrow-up-to-line"))
    val hideComposeButton by toggle(default = false, icon = Vector.lucide("square-plus"))
    val hideNotificationPrompts by toggle(default = true, icon = Vector.lucide("bell-off"))
    val blockInstalledAppsScan by toggle(default = true, icon = Vector.lucide("scan-search"))
    val disableAdTracking by toggle(default = true, icon = Vector.lucide("eye-off"))
    val featureSwitchOverrides by text(default = "", multiline = true, icon = Vector.lucide("flask-conical"))
    val canonicalLinks by toggle(default = true, icon = Vector.lucide("link"))
    val customUrlHandling by toggle(default = true, icon = Vector.lucide("external-link"))
    val showImportOnLaunch by toggle(default = true, icon = Vector.lucide("download"))
    val exportLogin by action(icon = Vector.lucide("copy"))

    val shareLinkProvider by
        choice(
            default = "off",
            choices =
                listOf(
                    Choice("fixvx.com", XStrings.shareLinkProviderFixvxCom),
                    Choice("fxtwitter.com", XStrings.shareLinkProviderFxtwitterCom),
                    Choice("vxtwitter.com", XStrings.shareLinkProviderVxtwitterCom),
                    Choice("fixupx.com", XStrings.shareLinkProviderFixupxCom),
                    Choice("off", XStrings.shareLinkProviderOff),
                ),
            icon = Vector.lucide("share-2"),
        )

    val postTranslator by
        choice(
            default = "grok",
            choices =
                listOf(
                    Choice("none", XStrings.postTranslatorNone),
                    Choice("grok", XStrings.postTranslatorGrok),
                    Choice("reseam", XStrings.postTranslatorReseam),
                ),
            icon = Vector.lucide("languages"),
        )

    val darkTheme by
        choice(
            default = "lights_out",
            choices =
                listOf(
                    Choice("lights_out", XStrings.darkThemeLightsOut),
                    Choice("dim", XStrings.darkThemeDim),
                ),
            icon = Vector.lucide("moon"),
        )

    val accentColor by
        choice(
            default = "x",
            choices =
                listOf(
                    Choice("x", XStrings.accentColorX),
                    Choice("material_you", XStrings.accentColorMaterialYou),
                    Choice("custom", XStrings.accentColorCustom),
                ),
            icon = Vector.lucide("palette"),
        )

    val customAccentColor by text(default = "#1D9BF0", icon = Vector.lucide("pipette"))
    val twitterBranding by toggle(default = false, icon = Vector.lucide("bird"))

    val defaultMediaTab by
        choice(
            default = "off",
            choices =
                listOf(
                    Choice("photos", XStrings.defaultMediaTabPhotos),
                    Choice("videos", XStrings.defaultMediaTabVideos),
                    Choice("off", XStrings.defaultMediaTabOff),
                ),
            icon = Vector.lucide("images"),
        )

    val firstTab by choice(default = "home", choices = navigationTabs, icon = Vector.lucide("panel-bottom"))
    val secondTab by choice(default = "explore", choices = navigationTabs, icon = Vector.lucide("panel-bottom"))
    val thirdTab by choice(default = "grok", choices = navigationTabs, icon = Vector.lucide("panel-bottom"))
    val fourthTab by choice(default = "notifications", choices = navigationTabs, icon = Vector.lucide("panel-bottom"))
    val fifthTab by choice(default = "dm", choices = navigationTabs, icon = Vector.lucide("panel-bottom"))
}

private val navigationTabs =
    listOf(
        Choice("home", XStrings.navigationTabHome),
        Choice("explore", XStrings.navigationTabExplore),
        Choice("grok", XStrings.navigationTabGrok),
        Choice("notifications", XStrings.navigationTabNotifications),
        Choice("dm", XStrings.navigationTabMessages),
        Choice("communities", XStrings.navigationTabCommunities),
        Choice("spaces", XStrings.navigationTabSpaces),
        Choice("none", XStrings.navigationTabNone),
    )
