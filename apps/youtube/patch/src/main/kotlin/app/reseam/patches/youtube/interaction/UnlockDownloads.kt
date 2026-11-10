// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.interaction

import app.reseam.patch.before
import app.reseam.patch.classTarget
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patch.settings.whenEnabled
import app.reseam.patches.youtube.YouTubePatchStrings
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.patches.youtube.misc.spoofVideoStreams
import app.reseam.patches.youtubecommon.parseProtobuf
import app.reseam.patches.youtubecommon.protobufBytes
import app.reseam.patches.youtubespoof.answerRefusedDownloads
import app.reseam.youtube.downloads.bindings.DownloadOptions
import app.reseam.youtube.downloads.bindings.QualityPicker
import app.reseam.youtube.settings.DownloadSettings
import app.reseam.youtube.settings.YouTubeStrings

private val downloadActionCommand = method("download action command") {
    strings("Unknown command")
    flags(AccessFlags.PRIVATE)
    paramCount(2)
}

private val downloadActionResolver = classTarget("download action resolver") {
    bytecode.findClass(downloadActionCommand.owner) ?: error("The download action resolver is missing")
}

private val downloadActionResponseClass = classTarget("download action response") {
    bytecode.findClass(downloadActionCommand.parameterTypes[1]) ?: error("The download action response is missing")
}

private val downloadActionResponse = method("download action response handler") {
    inClass(downloadActionResolver)
    calls(downloadActionCommand)
    paramCount(2)
    param(0, downloadActionCommand.parameterTypes[1])
}

private val downloadActionRequest = method("download action request") {
    inClass(downloadActionResolver)
    params(downloadActionCommand.parameterTypes[0], "java.util.Map")
}

val unlockDownloads = patch(YouTubePatchStrings.unlockDownloads) {
    description(YouTubePatchStrings.unlockDownloadsDescription)
    compatibleWith(YOUTUBE)
    dependsOn(youTubeSettings, spoofVideoStreams)
    settings(
        youTubeSettings,
        section(YouTubeSettingsPages.Downloads, YouTubeStrings.sectionUnlockDownloads, DownloadSettings.unlockDownloads),
    )

    execute {
        answerRefusedDownloads(DownloadSettings.unlockDownloads)
        downloadActionRequest.before {
            whenEnabled(DownloadSettings.unlockDownloads) { QualityPicker.requested(protobufBytes(param(0))) }
        }
        downloadActionResponse.before {
            whenEnabled(DownloadSettings.unlockDownloads) {
                val unlocked = DownloadOptions.unlock(protobufBytes(param(0)))
                whenNotNull(unlocked) {
                    param(0).assign(parseProtobuf(downloadActionResponseClass, "download action response", unlocked))
                }
            }
        }
    }
}
