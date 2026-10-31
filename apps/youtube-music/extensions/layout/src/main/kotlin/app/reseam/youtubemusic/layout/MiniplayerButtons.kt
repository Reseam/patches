// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("MiniplayerButtons")

package app.reseam.youtubemusic.layout

import android.media.AudioManager
import android.view.KeyEvent
import android.view.View
import app.reseam.bundle.identifier
import app.reseam.runtime.Export
import app.reseam.runtime.ToggleValue
import app.reseam.youtubemusic.settings.YouTubeMusicSettings

@Export
fun bind(playButton: View) {
    val row =
        checkNotNull(playButton.parent as View?) { "The miniplayer play button must have a row" }
    row.bindSkipButton(
        "reseam_mini_player_previous",
        YouTubeMusicSettings.miniplayerPreviousButton,
        KeyEvent.KEYCODE_MEDIA_PREVIOUS,
    )
    row.bindSkipButton(
        "reseam_mini_player_next",
        YouTubeMusicSettings.miniplayerNextButton,
        KeyEvent.KEYCODE_MEDIA_NEXT,
    )
}

private fun View.bindSkipButton(id: String, setting: ToggleValue, key: Int) {
    val button = findViewById<View>(context.identifier(id, "id")) ?: return
    button.visibility = if (setting.value) View.VISIBLE else View.GONE
    // The app's media session handles these keys just as it handles headset buttons.
    button.setOnClickListener { view ->
        val audio =
            checkNotNull(view.context.getSystemService(AudioManager::class.java)) {
                "Media keys require AudioManager"
            }
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, key))
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, key))
    }
}
