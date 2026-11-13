// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.hide

import app.reseam.patch.PatchRuntime
import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.method
import app.reseam.patch.methods
import app.reseam.patch.points
import app.reseam.patch.patch
import app.reseam.patch.resourceRef
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.YouTubePatchStrings
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.patches.youtube.internal.engagementPanelHook
import app.reseam.patches.youtube.internal.navigationBarHook
import app.reseam.patches.youtube.internal.playerTypeHook
import app.reseam.patches.youtubecommon.lithoFilter
import app.reseam.patches.youtubecommon.registerLithoFilter
import app.reseam.youtube.settings.ShortsSettings
import app.reseam.youtube.settings.YouTubeStrings
import app.reseam.youtube.shorts.bindings.ShortsFilter
import app.reseam.youtube.shorts.bindings.ShortsGestureHints

val hideShortsComponents = patch(YouTubePatchStrings.hideShortsComponents) {
    description(YouTubePatchStrings.hideShortsComponentsDescription)
    compatibleWith(YOUTUBE)

    val hideShortsAppShortcut = boolOption(
        "hideShortsAppShortcut",
        title = YouTubePatchStrings.hideShortsComponentsHideShortsAppShortcut,
        description = YouTubePatchStrings.hideShortsComponentsHideShortsAppShortcutDescription,
        default = false,
    )
    val hideShortsWidget = boolOption(
        "hideShortsWidget",
        title = YouTubePatchStrings.hideShortsComponentsHideShortsWidget,
        description = YouTubePatchStrings.hideShortsComponentsHideShortsWidgetDescription,
        default = false,
    )

    dependsOn(lithoFilter, engagementPanelHook, playerTypeHook, navigationBarHook, youTubeSettings)
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
            ShortsSettings.hideShortsGestureHints,
            ShortsSettings.hideShortsGreenScreenButton,
            ShortsSettings.hideShortsNewPostsButton,
            ShortsSettings.hideShortsHashtagButton,
            ShortsSettings.hideShortsHome,
            ShortsSettings.hideShortsSubscriptions,
            ShortsSettings.hideShortsSearch,
            ShortsSettings.hideShortsInfoPanel,
            ShortsSettings.hideShortsJoinButton,
            ShortsSettings.hideShortsLikeButton,
            ShortsSettings.hideShortsLikeFountain,
            ShortsSettings.hideShortsLivePreview,
            ShortsSettings.hideShortsLocationLabel,
            ShortsSettings.hideShortsPausedOverlayButtons,
            ShortsSettings.hideShortsPreviewComment,
            ShortsSettings.hideShortsRemixButton,
            ShortsSettings.hideShortsSaveButton,
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

        hideSpeedHints()
    }
}

// The 2x speed hint stub comes from the overlay layout or is built in code, depending on the player.
private fun PatchRuntime.hideSpeedHints() {
    // Releases from before the 2x hints have no stub for them.
    val stubId = resources.id("id", "reel_speedmaster_edu_stub")?.toLong() ?: return
    val owners = methods("Shorts speed hint stub owners") { literals(stubId) }
    val built = owners.points("built speed hint stub") {
        invokeVirtual { owner("android.view.ViewStub"); name("setId") }
        argument(1) { literal(stubId) }
    }.all
    val found = owners.points("found speed hint stub") {
        invokeVirtual { name("findViewById"); params(Type.Int); returns(Type.View) }
        argument(1) { literal(stubId) }
    }.all
    check(built.isNotEmpty() || found.isNotEmpty()) { "No Shorts speed hint stub" }
    built.forEach {
        it.captureArgumentAs("stub", 0, Type.View).after { ShortsGestureHints.hideGestureHints(capture("stub")) }
    }
    found.forEach {
        it.next { resultOf(Type.View) }.captureAs("stub", Type.View).after {
            ShortsGestureHints.hideGestureHints(capture("stub"))
        }
    }
}
