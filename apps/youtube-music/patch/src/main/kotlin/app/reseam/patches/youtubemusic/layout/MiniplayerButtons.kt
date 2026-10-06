// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtubemusic.layout

import app.reseam.patch.ExtClass
import app.reseam.patch.Type
import app.reseam.patch.XmlElement
import app.reseam.patch.after
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.points
import app.reseam.patch.resourceRef
import app.reseam.patch.settings.section
import app.reseam.patches.youtubemusic.core.YOUTUBE_MUSIC
import app.reseam.patches.youtubemusic.core.YouTubeMusicSettings
import app.reseam.patches.youtubemusic.core.YouTubeMusicSettingsPages
import app.reseam.patches.youtubemusic.core.youTubeMusicSettings

val miniplayerButtons = patch("Miniplayer skip buttons") {
    description("Adds previous and next buttons to the miniplayer.")
    compatibleWith(YOUTUBE_MUSIC)
    settings(
        youTubeMusicSettings,
        section(YouTubeMusicSettingsPages.Player, "Miniplayer",
            YouTubeMusicSettings.miniplayerPreviousButton, YouTubeMusicSettings.miniplayerNextButton),
    )

    execute {
        val playButtonId = resources.id("id", "mini_player_play_pause_replay_button") ?: error("id/mini_player_play_pause_replay_button is missing")
        val previousId = resources.addId("reseam_mini_player_previous") ?: error("Could not add id/reseam_mini_player_previous")
        val nextId = resources.addId("reseam_mini_player_next") ?: error("Could not add id/reseam_mini_player_next")

        // Copies of the play button keep its class, style, padding and size.
        resources.editXml("layout", "watch_while_layout") {
            val playButton = root.descendants().single { it["android:id"]?.let(::resourceRef) == playButtonId }
            val row = playButton.parent ?: error("The miniplayer play button has no parent")
            fun copy(id: UInt, icon: String, label: String) = playButton.clone().apply {
                setResourceRef("android:id", id)
                this["android:src"] = "@drawable/$icon"
                this["android:contentDescription"] = "@string/$label"
            }
            row.insertBefore(copy(previousId, "yt_fill_experimental_skip_previous_vd_theme_36", "accessibility_previous_track_enabled"), playButton)
            row.appendChild(copy(nextId, "yt_fill_experimental_skip_next_vd_theme_36", "accessibility_next_track_enabled"))
        }

        methods("miniplayer") { literals(playButtonId.toLong()) }.points("playButtonLookup") {
            invokeVirtual { name("findViewById"); params(Type.Int); returns(Type.View) }
            argument(1) { literal(playButtonId.toLong()) }
        }.single().next { resultOf(Type.View) }.captureAs("playButton").after {
            call(MiniplayerButtons.bind, capture("playButton"))
        }
    }
}

private fun XmlElement.descendants(): Sequence<XmlElement> = sequenceOf(this) + children.asSequence().flatMap { it.descendants() }

private object MiniplayerButtons : ExtClass("app.reseam.youtubemusic.layout.MiniplayerButtons") {
    val bind by static(Type.View)
}
