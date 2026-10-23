// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.interaction

import app.reseam.patch.before
import app.reseam.patch.classTarget
import app.reseam.patch.dex.Opcode
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.section
import app.reseam.patch.settings.whenEnabled
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
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
        ShortsDownloads.parseMenu.implement { returnValue(parseProtobuf(menuRenderer, "menu", param(0))) }

        // The Short's offlineability comes in its player response, which the menu does not carry.
        afterPlayerResponse { videoId, response ->
            whenEnabled(DownloadSettings.downloadShorts) { ShortsDownloads.onPlayerResponse(videoId, response) }
        }
        sheetItems.before {
            whenEnabled(DownloadSettings.downloadShorts) {
                val menu = ShortsDownloads.withDownloadItem(thisObject.field(sheetMenu))
                thisObject.set(sheetMenu, menu.cast(menuRenderer.descriptor))
            }
        }
    }
}

private val menuSheet = klass("menu bottom sheet") { strings("MENU_BOTTOM_SHEET_FRAGMENT_KEY", "VIDEO_QUALITY_DETAIL_TEXT") }

// Builds the sheet's rows from its menu renderer's items.
private val sheetItems = method("menu bottom sheet items") {
    inClass(menuSheet)
    strings("VIDEO_QUALITY_DETAIL_TEXT")
    params()
}

private val sheetMenu = sheetItems.point("menu renderer read") {
    opcode(Opcode.IGET_OBJECT)
    field { owner(menuSheet.descriptor) }
}.field("menu renderer")

private val menuRenderer = classTarget("menu renderer") {
    bytecode.findClass(sheetMenu.type) ?: error("The menu renderer class is missing")
}
