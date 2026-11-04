// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("AdEvents")

package app.reseam.twitch.adevents

import app.reseam.runtime.Export
import org.json.JSONException
import org.json.JSONObject

// Stitched ads, other ad formats, ad tracking, and the picture-by-picture ad preflight.
private val adMetadataClasses = setOf("twitch-stitched-ad", "twitch-maf-ad", "twitch-ad-quartile", "live-video-net-metadata")

@Export
fun isAdMetadata(metadata: JSONObject?): Boolean = metadata != null && metadata.optString("CLASS") in adMetadataClasses

@Export
fun isAdCue(cue: String?): Boolean {
    if (cue == null || adMetadataClasses.none { it in cue }) return false
    return try {
        isAdMetadata(JSONObject(cue))
    } catch (_: JSONException) {
        false
    }
}
