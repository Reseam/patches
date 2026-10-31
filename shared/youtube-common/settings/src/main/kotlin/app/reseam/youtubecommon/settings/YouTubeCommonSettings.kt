// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtubecommon.settings

import app.reseam.patch.settings.SettingsGroup
import app.reseam.patch.settings.text
import app.reseam.patch.settings.toggle

// Keys stay the ones YouTube shipped them under, which the shared extensions read.
object YouTubeCommonSettings : SettingsGroup(YouTubeCommonStrings, "you_tube_settings") {
    val customFilter by toggle(default = false)
    val customFilterStrings by text(default = "", multiline = true)
    val debugLogging by toggle(default = false)
    val sanitizeSharingLinks by toggle(default = true)
    val forceOriginalAudio by toggle(default = true)
}
