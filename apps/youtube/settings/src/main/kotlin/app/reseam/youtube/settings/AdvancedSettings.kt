// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.settings

import app.reseam.patch.settings.Choice
import app.reseam.patch.settings.SettingsGroup
import app.reseam.patch.settings.choice
import app.reseam.patch.settings.text
import app.reseam.patch.settings.toggle

object AdvancedSettings : SettingsGroup(YouTubeStrings, YOUTUBE_KEYS) {
    val featureFlagOverrides by text(default = "")
    val spoofDeviceDimensions by toggle(default = false)
    val openLinksExternally by toggle(default = true)
    val bypassUrlRedirects by toggle(default = true)
    val checkWatchHistoryDns by toggle(default = true)
    val spoofAppVersion by toggle(default = false)
    val spoofAppVersionTarget by
        choice(
            default = "20.05.46",
            choices =
                listOf(
                    Choice("20.13.41", YouTubeStrings.spoofAppVersionTarget201341),
                    Choice("20.05.46", YouTubeStrings.spoofAppVersionTarget200546),
                ),
        )
    val accountCredentialsInvalidText by toggle(default = true)
}
