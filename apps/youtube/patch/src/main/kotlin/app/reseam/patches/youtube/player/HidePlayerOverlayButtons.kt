// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.player

import app.reseam.patch.ExtClass
import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.before
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.Opcode
import app.reseam.patch.dex.literal
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.methodTarget
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.points
import app.reseam.patch.settings.ToggleSetting
import app.reseam.patch.settings.section
import app.reseam.patch.settings.skipWhen
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettings
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.patches.youtube.internal.overrideBooleanFeature

/** The initializer is shared with HideAutoplayPreview; both patches must edit the same method. */
val playerLayoutInitializer = methodTarget("player layout initializer") {
    val previous = resources.id("id", "player_control_previous_button_touch_area")?.toLong()
        ?: error("id/player_control_previous_button_touch_area is missing")
    method("player layout initializer") {
        flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
        params()
        returns(Type.Void)
        literals(previous)
    }.method
}

/** Both overlay patches guard the ViewStub call in this shared initializer. */
fun app.reseam.patch.MethodTarget.skipInflationWhen(
    resourceId: Long,
    setting: ToggleSetting,
) {
    point("ViewStub inflate for $resourceId") {
        literal(resourceId)
    }.next { checkCast("android.view.ViewStub") }.next {
        invokeVirtual {
            params("android.view.ViewStub", Type.Int)
            returns(Type.Void)
        }
    }.skipWhen(setting)
}

internal val subtitleButtonController = methodTarget("subtitle button controller") {
    val label = resources.id("string", "accessibility_captions_button_name")?.toLong()
        ?: error("string/accessibility_captions_button_name is missing")
    method("subtitle button controller") { literals(label); returns(Type.Void) }.method
}

val hidePlayerOverlayButtons = patch("Hide player overlay buttons") {
    description("Adds options to hide controls and buttons over the video player.")
    compatibleWith(YOUTUBE)
    dependsOn(youTubeSettings)
    settings(
        youTubeSettings,
        section(
            YouTubeSettingsPages.Overlay,
            "Player overlay",
            YouTubeSettings.hideAutoplayButton,
            YouTubeSettings.hideCaptionsButton,
            YouTubeSettings.hideCastButton,
            YouTubeSettings.hideCollapseButton,
            YouTubeSettings.hideFullscreenButton,
            YouTubeSettings.hidePlayerControlButtonsBackground,
            YouTubeSettings.hidePlayerPreviousNextButtons,
        ),
    )

    execute {
        val mediaRouteButton = klass("androidx.mediarouter.app.MediaRouteButton")
        mediaRouteButton.method("setVisibility") {
            params(Type.Int)
            returns(Type.Void)
        }.before {
            param(0).assign(call(HidePlayerOverlayButtons.castVisibility, param(0)))
        }

        overrideBooleanFeature(45690091L, HidePlayerOverlayButtons.castEnabled)
        overrideBooleanFeature(45690090L, HidePlayerOverlayButtons.castEnabled)

        // Read the button field directly; its surrounding null-check branches can change.
        subtitleButtonController.point("captions button") {
            opcode(Opcode.IGET_OBJECT)
            field { type("com.google.android.libraries.youtube.common.ui.TouchImageView") }
        }.captureAs("captionsButton", "android.widget.ImageView").after {
            call(HidePlayerOverlayButtons.hideCaptionsButton, capture("captionsButton"))
        }

        val previous = resources.id("id", "player_control_previous_button_touch_area")!!.toLong()
        playerLayoutInitializer
            .point("previous button parent") {
                invokeStatic { params(Type.View, Type.Int) }
                argument(1) { literal(previous) }
            }.captureArgumentAs("parent", 0, Type.View)
            .before { call(HidePlayerOverlayButtons.hidePreviousNextButtons, capture("parent")) }

        val autoplayToggle = resources.id("id", "autonav_toggle")?.toLong()
            ?: error("id/autonav_toggle is missing")
        playerLayoutInitializer.skipInflationWhen(
            autoplayToggle,
            YouTubeSettings.hideAutoplayButton,
        )

        val controlsGroup = resources.id("id", "youtube_controls_button_group_layout_stub")?.toLong()
            ?: error("id/youtube_controls_button_group_layout_stub is missing")
        playerLayoutInitializer
            .point("player controls group") { literal(controlsGroup) }
            .next { checkCast("android.view.ViewStub") }
            .next {
                invokeVirtual {
                    owner("android.view.ViewStub")
                    name("inflate")
                    params()
                    returns(Type.View)
                }
            }
            .next { resultOf(Type.View) }.captureAs("controls", Type.View).after {
                call(HidePlayerOverlayButtons.hidePlayerControlButtonsBackground, capture("controls"))
            }

        val collapse = resources.id("id", "player_collapse_button")?.toLong()
            ?: error("id/player_collapse_button is missing")
        val title = resources.id("id", "title_anchor")?.toLong()
            ?: error("id/title_anchor is missing")
        val titleAnchor = method("title and collapse buttons") {
            returns(Type.Void)
            literals(collapse, title)
        }
        titleAnchor
            .point("collapse button") { literal(collapse) }
            .next {
                checkCast("com.google.android.libraries.youtube.common.ui.TouchImageView")
            }
            .captureAs("collapseButton", "android.widget.ImageView")
            .after { call(HidePlayerOverlayButtons.hideCollapseButton, capture("collapseButton")) }

        titleAnchor
            .point("title anchor") { literal(title) }
            .next { resultOf(Type.View) }
            .captureAs("titleAnchor", Type.View)
            .after { call(HidePlayerOverlayButtons.setTitleAnchorStartMargin, capture("titleAnchor")) }

        val fullscreen = resources.id("id", "fullscreen_button")?.toLong()
            ?: error("id/fullscreen_button is missing")
        val fullscreenButtons = methods("fullscreen buttons") {
            flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
            params(Type.View)
            returns(Type.Void)
            literals(fullscreen)
        }
        // YouTube has the same resource lookup in two interchangeable overlay renderers; both
        // sites must be guarded so the button is absent regardless of which renderer is active.
        fullscreenButtons.points("fullscreen lookup") {
            invokeVirtual { name("findViewById"); params(Type.Int); returns(Type.View) }
            argument(1) { literal(fullscreen) }
        }.forEach {
            next { resultOf(Type.View) }.captureAs("fullscreen", Type.View).after {
                capture("fullscreen").assign(call(HidePlayerOverlayButtons.hideFullscreenButton, capture("fullscreen")))
                whenNull(capture("fullscreen")) { returnVoid() }
            }
        }
    }
}

object HidePlayerOverlayButtons : ExtClass("app.reseam.youtube.playerui.HidePlayerOverlayButtons") {
    val castVisibility = static("getCastButtonOverrideV2", Type.Int, returns = Type.Int)
    val castEnabled = static("getCastButtonOverrideV2", Type.Boolean, returns = Type.Boolean)
    val hideCaptionsButton = static("hideCaptionsButton", "android.widget.ImageView")
    val hideCollapseButton = static("hideCollapseButton", "android.widget.ImageView")
    val setTitleAnchorStartMargin = static("setTitleAnchorStartMargin", Type.View)
    val hidePreviousNextButtons = static("hidePreviousNextButtons", Type.View)
    val hideFullscreenButton = static("hideFullscreenButton", Type.View, returns = Type.View)
    val hidePlayerControlButtonsBackground = static(
        "hidePlayerControlButtonsBackground",
        Type.View,
    )
}
