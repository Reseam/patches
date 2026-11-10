// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.misc

import app.reseam.patch.Type
import app.reseam.patch.fieldOfType
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.YouTubePatchStrings
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.youtube.misc.bindings.PauseOnAudioInterrupt
import app.reseam.youtube.settings.VideoSettings
import app.reseam.youtube.settings.YouTubeStrings

val pauseOnAudioInterrupt = patch(YouTubePatchStrings.pauseOnAudioInterrupt) {
    description(YouTubePatchStrings.pauseOnAudioInterruptDescription)
    compatibleWith(YOUTUBE)
    settings(youTubeSettings, section(YouTubeSettingsPages.Video, YouTubeStrings.sectionAudio, VideoSettings.pauseOnAudioInterrupt))

    execute {
        // Asking the system not to duck is the clean half: it makes Android send a full transient
        // loss instead of a duck request.
        val builder = klass(audioFocusRequestBuilder.owner)
        gate(VideoSettings.pauseOnAudioInterrupt) {
            audioFocusRequestBuilder.before {
                thisObject.set(builder.fieldOfType(Type.Boolean), bool(true))
            }
        }
        // Not every audio source honours that, so a duck that arrives anyway is retold as a loss,
        // once per interruption.
        gate(VideoSettings.pauseOnAudioInterrupt) {
            audioFocusChangeListener.before {
                param(0).assign(PauseOnAudioInterrupt.onAudioFocusChange(param(0)))
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
