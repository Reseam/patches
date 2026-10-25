// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.interaction

import app.reseam.patch.before
import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patch.settings.whenEnabled
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.patches.youtube.internal.menuSheetMenu
import app.reseam.patches.youtube.internal.menuSheetRenderer
import app.reseam.patches.youtube.internal.menuSheetRows
import app.reseam.patches.youtube.internal.videoInformationHook
import app.reseam.patches.youtubecommon.parseProtobuf
import app.reseam.patches.youtubecommon.protobufBytes
import app.reseam.patches.youtubespoof.afterPlayerResponse
import app.reseam.youtube.downloads.bindings.ShortsDownloads
import app.reseam.youtube.settings.DownloadSettings
import app.reseam.youtube.settings.YouTubeStrings

private const val MESSAGE_LITE = "com.google.protobuf.MessageLite"

val downloadShorts = patch("Download Shorts") {
    description("Adds Download to the Shorts player's menu, using the app's own download flow.")
    compatibleWith(YOUTUBE)
    // Shorts are offered the qualities Unlock downloads offers, and download through its answers.
    dependsOn(youTubeSettings, videoInformationHook, unlockDownloads)
    settings(
        youTubeSettings,
        section(YouTubeSettingsPages.Downloads, YouTubeStrings.sectionUnlockDownloads, DownloadSettings.downloadShorts),
    )

    execute {
        ShortsDownloads.messageBytes.implement { returnValue(protobufBytes(param(0).cast(MESSAGE_LITE))) }
        ShortsDownloads.parseMenu.implement { returnValue(parseProtobuf(menuSheetRenderer, "menu", param(0))) }

        // The Short's offlineability comes in its player response, which the menu does not carry.
        afterPlayerResponse { videoId, response ->
            whenEnabled(DownloadSettings.downloadShorts) { ShortsDownloads.onPlayerResponse(videoId, response) }
        }
        menuSheetRows.before {
            whenEnabled(DownloadSettings.downloadShorts) {
                val menu = ShortsDownloads.withDownloadItem(thisObject.field(menuSheetMenu))
                thisObject.set(menuSheetMenu, menu.cast(menuSheetRenderer.descriptor))
            }
        }
    }
}
