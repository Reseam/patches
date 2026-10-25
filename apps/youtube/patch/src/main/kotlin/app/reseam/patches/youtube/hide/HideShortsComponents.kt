// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.hide

import app.reseam.patch.Type
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.resourceRef
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.patches.youtube.internal.engagementPanelHook
import app.reseam.patches.youtube.internal.playerTypeHook
import app.reseam.patches.youtubecommon.lithoFilter
import app.reseam.patches.youtubecommon.registerLithoFilter
import app.reseam.youtube.settings.ShortsSettings
import app.reseam.youtube.settings.YouTubeStrings
import app.reseam.youtube.shorts.bindings.ShortsFilter

val hideShortsComponents = patch("Hide Shorts components") {
    description("Hides Shorts shelves and configurable components in the Shorts player.")
    compatibleWith(YOUTUBE)

    val hideShortsAppShortcut = boolOption(
        "hideShortsAppShortcut",
        title = "Hide Shorts app shortcut",
        description = "Permanently removes the Shorts shortcut from the launcher.",
        default = false,
    )
    val hideShortsWidget = boolOption(
        "hideShortsWidget",
        title = "Hide Shorts widget",
        description = "Permanently removes the Shorts button from YouTube's widget.",
        default = false,
    )

    dependsOn(lithoFilter, engagementPanelHook, playerTypeHook, youTubeSettings)
    settings(
        youTubeSettings,
        section(
            YouTubeSettingsPages.Shorts,
            YouTubeStrings.sectionShorts,
            ShortsSettings.turnOffShorts,
            ShortsSettings.hideShortsAiButton,
            ShortsSettings.hideShortsAutoDubbedLabel,
            ShortsSettings.hideShortsChannel,
            ShortsSettings.hideShortsChannelBar,
            ShortsSettings.hideShortsCommentsButton,
            ShortsSettings.hideShortsDislikeButton,
            ShortsSettings.hideShortsFullVideoLinkLabel,
            ShortsSettings.hideShortsEffectButton,
            ShortsSettings.hideShortsGreenScreenButton,
            ShortsSettings.hideShortsNewPostsButton,
            ShortsSettings.hideShortsHashtagButton,
            ShortsSettings.hideShortsHome,
            ShortsSettings.hideShortsInfoPanel,
            ShortsSettings.hideShortsJoinButton,
            ShortsSettings.hideShortsLikeButton,
            ShortsSettings.hideShortsLikeFountain,
            ShortsSettings.hideShortsLivePreview,
            ShortsSettings.hideShortsLocationLabel,
            ShortsSettings.hideShortsPausedOverlayButtons,
            ShortsSettings.hideShortsPreviewComment,
            ShortsSettings.hideShortsRemixButton,
            ShortsSettings.hideShortsSaveSoundButton,
            ShortsSettings.hideShortsSearchSuggestions,
            ShortsSettings.hideShortsShareButton,
            ShortsSettings.hideShortsShopButton,
            ShortsSettings.hideShortsSoundButton,
            ShortsSettings.hideShortsSoundMetadataLabel,
            ShortsSettings.hideShortsStickers,
            ShortsSettings.hideShortsSubscribeButton,
            ShortsSettings.hideShortsSuperThanksButton,
            ShortsSettings.hideShortsTaggedProducts,
            ShortsSettings.hideShortsUpcomingButton,
            ShortsSettings.hideShortsUseSoundButton,
            ShortsSettings.hideShortsUseTemplateButton,
            ShortsSettings.hideShortsVideoDescription,
            ShortsSettings.hideShortsVideoTitle,
        ),
    )

    execute {
        registerLithoFilter(ShortsFilter)

        resources.editXml("xml", "main_shortcuts") {
            val item = findByAttribute("android:shortcutId", "shorts-shortcut").singleOrNull()
                ?: error("the Shorts launcher shortcut is missing")
            if (options[hideShortsAppShortcut]) item.remove()
        }

        resources.editXml("layout", "appwidget_two_rows") {
            val widgetButtonId = resources.id("id", "button_shorts_container")
                ?: error("id/button_shorts_container is missing")
            fun findByResourceId(element: app.reseam.patch.XmlElement): List<app.reseam.patch.XmlElement> = buildList {
                if (resourceRef(element["android:id"].orEmpty()) == widgetButtonId) add(element)
                element.children.forEach { addAll(findByResourceId(it)) }
            }
            val item = findByResourceId(root).singleOrNull()
                ?: error("the Shorts widget button is missing")
            if (options[hideShortsWidget]) item.remove()
        }

        // Sound controls in this release are Litho components, filtered by ShortsFilter.
    }
}

