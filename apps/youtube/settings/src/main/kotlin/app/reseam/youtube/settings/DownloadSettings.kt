// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.settings

import app.reseam.patch.settings.SettingsGroup
import app.reseam.patch.settings.text
import app.reseam.patch.settings.toggle

object DownloadSettings : SettingsGroup(YouTubeStrings, YOUTUBE_KEYS) {
    val externalDownloader by toggle(default = false)
    val externalDownloaderActionButton by toggle(default = false)
    val unlockDownloads by toggle(default = true)
    val downloadShorts by toggle(default = true)
    val externalDownloaderPackageName by text(default = "com.deniscerri.ytdl")
}
