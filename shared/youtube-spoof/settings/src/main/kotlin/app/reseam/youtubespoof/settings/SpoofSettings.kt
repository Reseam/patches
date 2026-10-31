// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtubespoof.settings

import app.reseam.patch.settings.Choice
import app.reseam.patch.settings.SettingsGroup
import app.reseam.patch.settings.choice
import app.reseam.patch.settings.text
import app.reseam.patch.settings.toggle

// Keys stay the ones YouTube shipped them under, which the shared extensions read.
object SpoofSettings : SettingsGroup(SpoofStrings, "you_tube_settings") {
    // Shared accessors for existing storage; neither declaration adds a settings UI entry.
    val spoofVideoStreamsStatsForNerds by toggle(default = true)
    val tokenServerCredential by text(default = "")

    val spoofVideoStreams by toggle(default = true)
    val spoofVideoStreamsClient by
        choice(
            default = "tv",
            choices =
                listOf(
                    Choice("tv", SpoofStrings.spoofVideoStreamsClientTv),
                    Choice("android", SpoofStrings.spoofVideoStreamsClientAndroid),
                    Choice("web", SpoofStrings.spoofVideoStreamsClientWeb),
                    Choice("visionos", SpoofStrings.spoofVideoStreamsClientVisionos),
                    Choice("tv_simply", SpoofStrings.spoofVideoStreamsClientTvSimply),
                ),
        )
    val userAgentClientSpoof by toggle(default = true)
}
