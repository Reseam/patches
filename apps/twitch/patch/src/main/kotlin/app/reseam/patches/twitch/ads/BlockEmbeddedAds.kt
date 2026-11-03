// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.twitch.ads

import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.before
import app.reseam.patch.field
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patch.settings.valueOf
import app.reseam.patches.twitch.core.TWITCH_PACKAGE
import app.reseam.patches.twitch.core.twitchSettings
import app.reseam.twitch.embeddedads.bindings.EmbeddedAds
import app.reseam.twitch.settings.AdsSettings
import app.reseam.twitch.settings.TwitchStrings

// The player loads streams through the IVS network classes, which keep their names because native code calls them
val ivsRequest = klass("com.amazonaws.ivs.net.Request")
val ivsResponse = klass("com.amazonaws.ivs.net.Response")
val ivsResponseCallback = klass($$"com.amazonaws.ivs.net.OkHttpClient$OkHttpResponseCallback")

val requestUrl = ivsRequest.method("getUrl")
val requestHeaders = ivsRequest.method("getHeaders")
val requestUrlField = ivsRequest.field("url")
val responseCreate = ivsResponse.method("<init>") { params(Type.Int, Type.String) }
val requestFailure = ivsResponseCallback.method("onFailure")
val callbackRequest = ivsResponseCallback.field("request")

@Suppress("unused")
val blockEmbeddedAds = patch("Block embedded ads") {
    description("Loads live streams through a third-party proxy in a region without ads, which sees your IP address and the channels you watch, and switches to Twitch for a few minutes when the proxy fails.")
    compatibleWith(TWITCH_PACKAGE)
    enabledByDefault(false)
    settings(twitchSettings, section(TwitchStrings.sectionAds, AdsSettings.blockEmbeddedAds, AdsSettings.embeddedAdsProxy))

    execute {
        EmbeddedAds.proxyUrl.implement { returnValue(valueOf(AdsSettings.embeddedAdsProxy)) }

        gate(AdsSettings.blockEmbeddedAds) {
            requestUrl.after { returnValue(EmbeddedAds.playlistUrl(result)) }

            // The proxy gets none of the player's headers, the same way it gets none of its tokens
            requestHeaders.after { returnValue(EmbeddedAds.playlistHeaders(thisObject.field(requestUrlField), result)) }
        }

        // Only proxied requests count, so these stay quiet while the setting is off
        responseCreate.after { EmbeddedAds.onResponse(param(0), param(1)) }
        requestFailure.before { EmbeddedAds.onFailure(thisObject.field(callbackRequest).call(requestUrl)) }
    }
}
