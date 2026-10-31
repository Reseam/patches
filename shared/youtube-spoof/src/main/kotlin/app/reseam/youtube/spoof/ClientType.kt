// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.spoof

import android.net.Uri
import app.reseam.youtube.web.WebPlayer
import app.reseam.youtubespoof.settings.SpoofStrings

internal enum class ClientType(
    val settingValue: String,
    val clientName: String,
    val clientId: Int,
    val userAgent: String,
    val friendlyName: String,
    val usesWebPlayer: Boolean,
    val sabr: Boolean,
    private val displayLabel: () -> String,
    val clientVersion: String? = null,
    val deviceMake: String? = null,
    val deviceModel: String? = null,
    val osName: String? = null,
    val osVersion: String? = null,
    // Keeps the app's own streams and supplies the integrity token the native client cannot mint,
    // rather than requesting streams from an alternate client.
    val native: Boolean = false,
    // Returns every audio track whatever the request language; otherwise only the track matching it.
    val multiAudioTracks: Boolean = false,
) {
    TV(
        "tv",
        "TVHTML5",
        7,
        TvProfile.USER_AGENT,
        "TV (SABR)",
        true,
        true,
        { SpoofStrings.spoofClientTv },
        multiAudioTracks = true,
    ),
    ANDROID(
        "android",
        "ANDROID",
        3,
        "com.google.android.youtube/21.40.166 (Linux; U; Android 15) gzip",
        "Android",
        false,
        true,
        { SpoofStrings.spoofClientAndroid },
        native = true,
        multiAudioTracks = true,
    ),
    WEB(
        "web",
        "WEB",
        1,
        WebPlayer.USER_AGENT,
        "Web (SABR)",
        true,
        true,
        { SpoofStrings.spoofClientWeb },
        multiAudioTracks = true,
    ),
    VISIONOS(
        "visionos",
        "VISIONOS",
        101,
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 15_7_3) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/26.0 Safari/605.1.15",
        "visionOS",
        false,
        false,
        { SpoofStrings.spoofClientVisionos },
        clientVersion = "1.02",
        deviceMake = "Apple",
        deviceModel = "RealityDevice17,1",
        osName = "visionOS",
        osVersion = "26.5.23O471",
    ),
    TV_SIMPLY(
        "tv_simply",
        "TVHTML5_SIMPLY",
        75,
        WebPlayer.USER_AGENT,
        "TV Simply",
        true,
        false,
        { SpoofStrings.spoofClientTvSimply },
        clientVersion = "1.0",
    );

    val displayName: String
        get() = displayLabel()
}

internal fun issuedDirectUrl(uri: Uri): Boolean {
    if (uri.path != "/videoplayback") return false
    val host = uri.host ?: return false
    val name = uri.getQueryParameter("c") ?: return false
    return host.endsWith(".googlevideo.com") &&
        ClientType.entries.any { !it.sabr && it.clientName == name }
}

internal fun clientFromSetting(value: String): ClientType =
    ClientType.entries.firstOrNull { it.settingValue == value } ?: ClientType.WEB
