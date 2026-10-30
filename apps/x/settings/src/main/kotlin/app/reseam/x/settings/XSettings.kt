// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.x.settings

import app.reseam.patch.settings.Choice
import app.reseam.patch.settings.SettingsGroup
import app.reseam.patch.settings.choice
import app.reseam.patch.settings.folder
import app.reseam.patch.settings.text
import app.reseam.patch.settings.toggle
import app.reseam.patches.bundle.lucide

object XSettings : SettingsGroup(XStrings) {
    val defaultToFollowing by toggle(default = false, icon = lucide("users"))
    val hidePromotionPrompts by toggle(default = true, icon = lucide("rocket"))
    val hideAds by toggle(default = true, icon = lucide("megaphone-off"))
    val hidePremiumUpsells by toggle(default = true, icon = lucide("gem"))
    val hideAppOpenPaywall by toggle(default = true, icon = lucide("lock"))
    val unlockPremium by toggle(default = true, icon = lucide("badge-check"))
    val downloadAnyVideo by toggle(default = true, icon = lucide("download"))
    val downloadWithoutWatermark by toggle(default = true, icon = lucide("droplet-off"))
    val downloadFolder by folder(default = "X", icon = lucide("folder-open"))
    val saveAllMedia by toggle(default = true, icon = lucide("images"))
    val downloadButton by toggle(default = true, icon = lucide("download"))

    val hideGrokTab by toggle(default = true, icon = lucide("bot-off"))
    val hideGrokPostButton by toggle(default = true, icon = lucide("sparkles"))
    val hideGrokDrawerEntry by toggle(default = true, icon = lucide("panel-left"))
    val hideGrokPrompts by toggle(default = true, icon = lucide("message-square-dashed"))
    val hideGrokImageTools by toggle(default = true, icon = lucide("image-off"))
    val hideGrokComposer by toggle(default = true, icon = lucide("pen-off"))
    val disableGrokTranslations by toggle(default = false, icon = lucide("languages"))
    val hideLiveBar by toggle(default = true, icon = lucide("radio"))
    val hideLiveAvatarRings by toggle(default = false, icon = lucide("circle-dot"))
    val hideRecommendations by toggle(default = false, icon = lucide("user-plus"))
    val hideViewCounts by toggle(default = false, icon = lucide("chart-no-axes-column"))
    val hideNewPostsBanner by toggle(default = false, icon = lucide("arrow-up-to-line"))
    val hideNotificationPrompts by toggle(default = true, icon = lucide("bell-off"))
    val blockInstalledAppsScan by toggle(default = true, icon = lucide("scan-search"))
    val disableAdTracking by toggle(default = true, icon = lucide("eye-off"))
    val featureSwitchOverrides by text(default = "", multiline = true, icon = lucide("flask-conical"))
    val canonicalLinks by toggle(default = true, icon = lucide("link"))
    val customUrlHandling by toggle(default = true, icon = lucide("external-link"))
    val showImportOnLaunch by toggle(default = true, icon = lucide("download"))

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
            icon = lucide("share-2"),
        )

    val defaultMediaTab by
        choice(
            default = "off",
            choices =
                listOf(
                    Choice("photos", XStrings.defaultMediaTabPhotos),
                    Choice("videos", XStrings.defaultMediaTabVideos),
                    Choice("off", XStrings.defaultMediaTabOff),
                ),
            icon = lucide("images"),
        )

    val firstTab by choice(default = "home", choices = navigationTabs, icon = lucide("panel-bottom"))
    val secondTab by choice(default = "explore", choices = navigationTabs, icon = lucide("panel-bottom"))
    val thirdTab by choice(default = "grok", choices = navigationTabs, icon = lucide("panel-bottom"))
    val fourthTab by choice(default = "notifications", choices = navigationTabs, icon = lucide("panel-bottom"))
    val fifthTab by choice(default = "dm", choices = navigationTabs, icon = lucide("panel-bottom"))
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
