// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.spoof

import app.reseam.youtube.web.WebPlayer
import java.util.Locale
import org.json.JSONObject

internal fun innertubeBody(
    client: ClientType,
    videoId: String,
    visitor: String?,
    locale: Locale,
    web: WebPlayer.Session?,
    attestation: WebPlayer.Attestation?,
    reloadToken: String?,
): String {
    val clientJson =
        attestation?.let { JSONObject(it.client.toString()) }
            ?: JSONObject()
                .put("deviceMake", client.deviceMake)
                .put("deviceModel", client.deviceModel)
                .put("clientName", client.clientName)
                .put("clientVersion", client.clientVersion)
                .put("osName", client.osName)
                .put("osVersion", client.osVersion)
                .put("userAgent", client.userAgent)
                .put("visitorData", visitor)
                .put("hl", locale.language)
                .put("gl", locale.country)
    val body =
        JSONObject()
            .put("context", JSONObject().put("client", clientJson))
            .put("contentCheckOk", true)
            .put("racyCheckOk", true)
            .put("videoId", videoId)
    if (web != null) {
        body.put(
            "playbackContext",
            JSONObject()
                .put(
                    "contentPlaybackContext",
                    JSONObject().put("signatureTimestamp", web.signatureTimestamp),
                ),
        )
    }
    if (attestation != null && reloadToken != null) {
        body
            .getJSONObject("playbackContext")
            .put(
                "reloadPlaybackContext",
                JSONObject().put("reloadPlaybackParams", JSONObject().put("token", reloadToken)),
            )
    }
    if (attestation != null && attestation.poToken.isNotEmpty()) {
        body.put("serviceIntegrityDimensions", JSONObject().put("poToken", attestation.poToken))
    }
    return body.toString()
}
