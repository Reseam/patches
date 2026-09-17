// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.layout

import app.reseam.patch.ExtClass
import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.points
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettings
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings

val miniplayer = patch("Miniplayer") {
    description("Changes the current miniplayer's width, buttons and subtext.")
    compatibleWith(YOUTUBE)
    settings(youTubeSettings, section(YouTubeSettingsPages.Player, "Miniplayer",
        YouTubeSettings.hideMiniplayerOverlayButtons,
        YouTubeSettings.hideMiniplayerSubtext,
        YouTubeSettings.miniplayerWidthDip,
    ))

    execute {
        val viewClass = klass(method("miniplayerView") {
            strings("player_overlay_modern_mini_player_controls")
            returns(Type.String)
        }.owner)
        listOf(
            "modern_miniplayer_close" to Miniplayer.hideOverlayButton,
            "modern_miniplayer_expand" to Miniplayer.hideOverlayButton,
            "modern_miniplayer_overlay_action_button" to Miniplayer.hideOverlayButton,
            "modern_miniplayer_subtitle_text" to Miniplayer.hideSubtext,
        ).forEach { (name, hook) ->
            val id = resources.id("id", name)?.toLong() ?: error("id/$name is missing")
            val views = viewClass.methods(name) { literals(id) }.points(name) {
                invokeVirtual { name("findViewById"); params(Type.Int); returns(Type.View) }
                argument(1) { literal(id) }
            }.all
            check(views.isNotEmpty()) { "No miniplayer lookup for $name" }
            views.forEach { view ->
                view.next { resultOf(Type.View) }.captureAs("view", Type.View).after {
                    call(hook, capture("view"))
                }
            }
        }

        val maxSize = resources.id("dimen", "miniplayer_max_size")?.toLong()
            ?: error("dimen/miniplayer_max_size is missing")
        val sizing = method("miniplayerSizing") {
            flags(AccessFlags.CONSTRUCTOR)
            literals(maxSize)
        }
        // The first 192dp conversion initializes the current width. The later conversion
        // initializes the resize minimum; leave that native constraint intact.
        sizing.point("initialMiniplayerWidth") { literal(192) }
            .next { invokeStatic { params("android.util.DisplayMetrics", Type.Int); returns(Type.Int) } }
            .next { resultOf(Type.Int) }.captureAs("width", Type.Int).after {
                capture("width").assign(call(Miniplayer.width, capture("width")))
            }
    }
}

object Miniplayer : ExtClass("app.reseam.youtube.miniplayer.Miniplayer") {
    val width = static("width", Type.Int, returns = Type.Int)
    val hideOverlayButton = static("hideOverlayButton", Type.View)
    val hideSubtext = static("hideSubtext", Type.View)
}
