// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("SpoofVideoStreams")

package app.reseam.youtube.spoof

import android.net.Uri
import app.reseam.bundle.Logger
import app.reseam.runtime.Export
import app.reseam.youtube.core.debug
import app.reseam.youtube.web.WebPlayer
import app.reseam.youtubecommon.settings.YouTubeCommonSettings
import app.reseam.youtubespoof.settings.SpoofSettings
import app.reseam.youtubespoof.settings.SpoofStrings

@Export
fun setClientOrderToUse() {
    val client = clientFromSetting(SpoofSettings.spoofVideoStreamsClient.value)
    StreamingDataRequest.setClientOrder(
        client,
        ClientType.entries,
        true,
        YouTubeCommonSettings.forceOriginalAudio.value,
    )
    if (isSpoofingEnabled() && StreamingDataRequest.usesWebPlayer()) WebPlayer.warmUp()
}

@Export fun isSpoofingEnabled(): Boolean = SpoofSettings.spoofVideoStreams.value

@Export
fun rewriteClientContextOsName(original: String?): String? {
    if (!isSpoofingEnabled()) return original
    val rewritten = StreamingDataRequest.clientOsName
    if (rewritten != null && rewritten != original) {
        Logger.debug { "Client-context OS name spoofed to $rewritten" }
        return rewritten
    }
    return original
}

// Downloads retain request identity even when playback spoofing is disabled.
@Export
fun fetchStreams(url: String?, requestHeaders: Map<String?, String?>?, requestBody: ByteArray?) {
    if (url == null) return
    try {
        val uri = Uri.parse(url)
        if (uri.host == "youtubei.googleapis.com") {
            StreamingDataRequest.authorization = requestHeaders?.get("Authorization")
        }
        val path = uri.path ?: return
        if (!path.contains("player")) return
        if (listOf("get_drm_license", "heartbeat", "ad_break").any { it in path }) return
        // Some apps send their offline requests through a path this hook does not see, so downloads use the
        // identity of the latest player request of any kind.
        StreamingDataRequest.setOfflineHeaders(requestHeaders)
        if (isOfflinePlayerRequest(requestBody)) return
        val videoId = uri.getQueryParameter("id")?.takeIf { it.isNotEmpty() } ?: return
        if (isSpoofingEnabled()) StreamingDataRequest.fetchRequest(videoId, requestHeaders, requestBody)
    } catch (exception: Exception) {
        Logger.error(exception) { "Spoof stream URL failure" }
    }
}

@Export
fun getStreamingData(videoId: String?): StreamingData? {
    if (!isSpoofingEnabled() || videoId == null) return null
    val response =
        awaitStreams(videoId)
            ?: run {
                Logger.debug { "Not overriding streaming data (video stream is null): $videoId" }
                return null
            }
    if (response.native) {
        InstalledStreams.installed(videoId, response)
        Logger.debug { "Keeping native streams for $videoId with a server integrity token" }
        return null
    }
    Logger.debug { "Overriding video stream: $videoId" }
    return response
}

@Export fun getStreamingDataBytes(data: StreamingData): ByteArray = data.response

@Export
fun onStreamingDataInstalled(videoId: String, data: StreamingData) {
    InstalledStreams.installed(videoId, data)
}

@Export
fun onNativeStreamingDataInstalled(videoId: String?) {
    InstalledStreams.nativeInstalled(videoId)
}

@Export
fun onVideoChanged(videoId: String?) {
    InstalledStreams.videoChanged(videoId)
}

@Export
fun removeVideoPlaybackPostBody(uri: Uri?, method: Int, postData: ByteArray?): ByteArray? {
    if (method == 2 && uri != null && issuedDirectUrl(uri)) {
        Logger.debug { "Removed Android video playback POST body for a direct stream URL" }
        return null
    }
    return postData
}

@Export
fun rewriteVideoPlaybackMethod(uri: Uri?, method: Int): Int =
    if (method == 2 && uri != null && issuedDirectUrl(uri)) 1 else method

@Export
fun appendSpoofedClient(original: String?): String? =
    if (
        isSpoofingEnabled() &&
            SpoofSettings.spoofVideoStreamsStatsForNerds.value &&
            !original.isNullOrEmpty()
    ) {
        SpoofStrings.spoofStatsClient(original, InstalledStreams.clientName())
    } else original
