// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.hide

import app.reseam.patch.ExtClass
import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.alwaysReturn
import app.reseam.patch.classTarget
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.resourceRef
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettings
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.patches.youtube.internal.conversionContextPath
import app.reseam.patches.youtube.internal.engagementPanelHook
import app.reseam.patches.youtube.internal.lithoFilter
import app.reseam.patches.youtube.internal.playerTypeHook
import app.reseam.patches.youtube.internal.registerLithoFilter

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
            "Shorts",
            YouTubeSettings.hideShortsAiButton,
            YouTubeSettings.hideShortsAutoDubbedLabel,
            YouTubeSettings.hideShortsChannel,
            YouTubeSettings.hideShortsChannelBar,
            YouTubeSettings.hideShortsCommentsButton,
            YouTubeSettings.hideShortsDislikeButton,
            YouTubeSettings.hideShortsFullVideoLinkLabel,
            YouTubeSettings.hideShortsEffectButton,
            YouTubeSettings.hideShortsGreenScreenButton,
            YouTubeSettings.hideShortsNewPostsButton,
            YouTubeSettings.hideShortsHashtagButton,
            YouTubeSettings.hideShortsHome,
            YouTubeSettings.hideShortsInfoPanel,
            YouTubeSettings.hideShortsJoinButton,
            YouTubeSettings.hideShortsLikeButton,
            YouTubeSettings.hideShortsLikeFountain,
            YouTubeSettings.hideShortsLivePreview,
            YouTubeSettings.hideShortsLocationLabel,
            YouTubeSettings.hideShortsPausedOverlayButtons,
            YouTubeSettings.hideShortsPreviewComment,
            YouTubeSettings.hideShortsRemixButton,
            YouTubeSettings.hideShortsSaveSoundButton,
            YouTubeSettings.hideShortsSearchSuggestions,
            YouTubeSettings.hideShortsShareButton,
            YouTubeSettings.hideShortsShopButton,
            YouTubeSettings.hideShortsSoundButton,
            YouTubeSettings.hideShortsSoundMetadataLabel,
            YouTubeSettings.hideShortsStickers,
            YouTubeSettings.hideShortsSubscribeButton,
            YouTubeSettings.hideShortsSuperThanksButton,
            YouTubeSettings.hideShortsTaggedProducts,
            YouTubeSettings.hideShortsUpcomingButton,
            YouTubeSettings.hideShortsUseSoundButton,
            YouTubeSettings.hideShortsUseTemplateButton,
            YouTubeSettings.hideShortsVideoDescription,
            YouTubeSettings.hideShortsVideoTitle,
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

        // In 20.22+ the action-bar children are returned together, so filtering the list by its
        // stable indices avoids confusing a comment/share/remix button with another component.
        run {
            val resultListMethod = method("shortsActionButtonResultList") {
                inClass(componentContextParserClass)
                flags(AccessFlags.PRIVATE or AccessFlags.FINAL)
                returns(Type.List)
                calls { owner("Ljava/util/Collections;"); name("nCopies"); returns(Type.List) }
            }
            resultListMethod.after {
                call(
                    ShortsFilter.hideActionButtons,
                    param(1).cast(conversionContextPath.owner).field(conversionContextPath),
                    capture("result"),
                )
            }
        }

        // The current player combines both experimental controls in one predicate.
        method("shortsNativeControls") {
            literals(45677719L, 45649743L)
            returns(Type.Boolean)
        }.alwaysReturn(false)
    }
}

val componentContextParser = method("componentContextParser") {
    strings("Cannot read theme key from model.")
}

val componentContextParserClass = classTarget("componentContextParserClass") {
    bytecode.findClass(componentContextParser.owner)
        ?: error("The component context parser class is missing")
}

object ShortsFilter : ExtClass("app.reseam.youtube.shorts.ShortsFilter") {
    val hideActionButtons = static(
        "hideActionButtons",
        "java.lang.StringBuilder",
        Type.List,
    )
}
