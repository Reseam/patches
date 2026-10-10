// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtubespoof

import app.reseam.patch.settings.Choice
import app.reseam.patch.settings.choice
import app.reseam.patch.settings.toggle

// Keys stay the ones YouTube shipped them under, which the shared extensions read.
object SpoofSettings {
    val spoofVideoStreams by toggle(
        "Spoof video streams",
        summary = "Requests playback streams using a compatible alternate YouTube client.",
        default = true,
        key = "you_tube_settings.spoof_video_streams",
    )
    val spoofVideoStreamsClient by choice(
        "Default client",
        summary = "The client asked first for playback streams; the others are tried when it returns none.",
        default = "tv",
        choices = listOf(
            Choice("tv", "TV (SABR)"),
            Choice("web", "Web (SABR)"),
            Choice("tv_simply", "TV Simply"),
            Choice("visionos", "visionOS"),
        ),
        key = "you_tube_settings.spoof_video_streams_client",
    )
    val spoofVideoStreamsTokenSource by choice(
        "Web token source",
        summary = "Where the Web client gets its playback tokens. Web is used when selected, and as a fallback " +
            "when signed out or when another client fails. The Reseam server is provided under the terms at " +
            "https://mint.reseam.app.",
        default = "device",
        choices = listOf(
            Choice("device", "This device"),
            Choice("server", "Reseam server"),
        ),
        key = "you_tube_settings.spoof_video_streams_token_source",
    )
    val userAgentClientSpoof by toggle(
        "Spoof user-agent client",
        summary = "Reports the app's original package name in generated client user agents.",
        default = true,
        key = "you_tube_settings.user_agent_client_spoof",
    )
}
