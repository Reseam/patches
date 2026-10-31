// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("PauseOnAudioInterrupt")

package app.reseam.youtube.misc

import android.media.AudioManager
import app.reseam.bundle.Logger
import app.reseam.runtime.Export
import app.reseam.youtube.core.debug

// A second duck after resuming must lower the volume rather than pause again.
private var pausedForDuck = false

@Export
fun onAudioFocusChange(focusChange: Int): Int =
    when (focusChange) {
        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
            if (pausedForDuck) {
                Logger.debug { "Resumed during an audio interrupt, lowering the volume instead" }
                focusChange
            } else {
                pausedForDuck = true
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
            }
        }
        AudioManager.AUDIOFOCUS_GAIN,
        AudioManager.AUDIOFOCUS_LOSS -> {
            pausedForDuck = false
            focusChange
        }
        else -> focusChange
    }
