// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.settings

import app.reseam.patch.settings.SettingsGroup
import app.reseam.patch.settings.text
import app.reseam.patch.settings.toggle

object FilterSettings : SettingsGroup(YouTubeStrings, YOUTUBE_KEYS) {
    val hideKeywordContentHome by toggle(default = false)
    val hideKeywordContentSubscriptions by toggle(default = false)
    val hideKeywordContentSearch by toggle(default = false)
    val hideKeywordContentPhrases by text(default = "", multiline = true)
}
