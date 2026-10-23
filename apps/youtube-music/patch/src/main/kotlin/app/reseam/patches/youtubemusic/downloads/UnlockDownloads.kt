// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtubemusic.downloads

import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.before
import app.reseam.patch.dex.Opcode
import app.reseam.patch.dex.ref
import app.reseam.patch.fieldTarget
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.reserveLocal
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.youtubemusic.core.YOUTUBE_MUSIC
import app.reseam.patches.youtubemusic.core.YouTubeMusicSettingsPages
import app.reseam.patches.youtubemusic.core.youTubeMusicSettings
import app.reseam.patches.youtubemusic.misc.spoofVideoStreams
import app.reseam.patches.youtubespoof.answerRefusedDownloads
import app.reseam.patches.youtubespoof.answerRefusedOfflineStates
import app.reseam.youtubemusic.downloads.bindings.OfflineUpsell
import app.reseam.youtubemusic.settings.YouTubeMusicSettings
import app.reseam.youtubemusic.settings.YouTubeMusicStrings

private const val INTERACTION_LOGGER = "com.google.android.libraries.youtube.logging.interaction_logger"
private const val ENDPOINT_TAG = "com.google.android.libraries.youtube.innertube.endpoint.tag"
private const val DOWNLOAD_STREAM_CONFIG = 3
private const val JAVA_KEY = "java.security.Key"

val unlockDownloads = patch("Unlock downloads") {
    description("Downloads songs, albums and playlists without Premium.")
    compatibleWith(YOUTUBE_MUSIC)
    dependsOn(spoofVideoStreams)
    settings(
        youTubeMusicSettings,
        section(YouTubeMusicSettingsPages.Playback, YouTubeMusicStrings.sectionDownloads, YouTubeMusicSettings.unlockDownloads),
    )

    execute {
        answerRefusedDownloads(YouTubeMusicSettings.unlockDownloads)
        answerRefusedOfflineStates(YouTubeMusicSettings.unlockDownloads)
        val upsellVideoId = offlineUpsellCommand.reserveLocal("upsellVideoId", Type.String)
        gate(YouTubeMusicSettings.unlockDownloads) {
            // Without a player config the downloader's data source falls back to the playing video's, which can
            // make it post ustreamer requests that the server refuses for another video's streams.
            downloadStream.before {
                val config = paramOfType(downloadStream.parameterTypes[DOWNLOAD_STREAM_CONFIG])
                whenNull(config) { config.assign(staticField(defaultPlayerConfig)) }
            }
            // A download item carries an upsell for free accounts; an entitled account's upsell downloads what
            // the item's tag names. A song menu's tag names no video, so a song's upsell supplies it.
            offlineUpsellCommand.point { opcode(Opcode.IGET_OBJECT); field { type(Type.String) } }
                .captureAs("params", Type.String)
                .after { local(upsellVideoId).assign(OfflineUpsell.videoId(capture("params"))) }
            offlineUpsellCommand.point { string(INTERACTION_LOGGER) }
                .next { invokeVirtual { returns(Type.Boolean); params() } }
                .next { resultOf(Type.Boolean) }
                .captureAs("entitled", Type.Boolean)
                .after { capture("entitled").assign(bool(true)) }
            offlineUpsellCommand.point { string(ENDPOINT_TAG) }
                .next { resultOf(Type.Object) }
                .captureAs("tag", Type.Object)
                .after { whenNotNull(local(upsellVideoId)) { capture("tag").assign(local(upsellVideoId)) } }
        }
    }
}

private val offlineUpsellRequest = klass("offline upsell request") { strings("ypc/get_offline_upsell") }

private val offlineUpsellCommand = method("offline upsell command") {
    calls(offlineUpsellRequest.method("<init>"))
    hasParam(Type.Map)
}

private val downloader = klass("offline downloader") {
    strings("Downloader")
    hasInstanceField(JAVA_KEY)
}

// Fetches one stream of a download: (stream, start, length, player config, ...).
private val downloadStream = method("download stream") {
    inClass(downloader)
    returns(Type.Void)
    paramCount(7)
    param(1, Type.Long)
    param(2, Type.Long)
    hasParam(Type.String)
}

private val defaultPlayerConfig = fieldTarget("default player config") {
    val config = downloadStream.parameterTypes[DOWNLOAD_STREAM_CONFIG]
    val configClass = bytecode.findClass(config) ?: error("The player config class is missing")
    configClass.staticFields.single { it.fieldType == config }.ref
}
