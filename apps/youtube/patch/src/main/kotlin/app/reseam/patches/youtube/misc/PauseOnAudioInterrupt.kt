// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.misc

import app.reseam.patch.Type
import app.reseam.patch.fieldOfType
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.before
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettings
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings

private const val AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK = -3
private const val AUDIOFOCUS_LOSS_TRANSIENT = -2

val pauseOnAudioInterrupt = patch("Pause on audio interrupt") {
    description("Pauses playback instead of lowering the volume when another app plays audio.")
    compatibleWith(YOUTUBE)
    settings(youTubeSettings, section(YouTubeSettingsPages.Video, "Audio", YouTubeSettings.pauseOnAudioInterrupt))

    execute {
        // Asking the system not to duck is the clean half: it makes Android send a full transient
        // loss instead of a duck request.
        val builder = klass(audioFocusRequestBuilder.owner)
        audioFocusRequestBuilder.before(YouTubeSettings.pauseOnAudioInterrupt) {
            thisObject.set(builder.fieldOfType(Type.Boolean), bool(true))
        }
        // Not every audio source honours that, so a duck that arrives anyway is retold as a loss.
        audioFocusChangeListener.before(YouTubeSettings.pauseOnAudioInterrupt) {
            whenEqual(param(0), int(AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK)) {
                param(0).assign(int(AUDIOFOCUS_LOSS_TRANSIENT))
            }
        }
    }
}

// The builder throws when it has no listener, which is the one thing it says out loud.
val audioFocusRequestBuilder = method("audioFocusRequestBuilder") {
    strings("Can't build an AudioFocusRequestCompat instance without a listener")
}

val audioFocusChangeListener = method("audioFocusChangeListener") {
    name("onAudioFocusChange")
    strings("AudioFocus DUCK", "AudioFocus loss; Will lower volume")
}
