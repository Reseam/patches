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
import app.reseam.patches.twitch.TwitchPatchStrings
import app.reseam.patches.twitch.core.TWITCH
import app.reseam.patches.twitch.core.twitchSettings
import app.reseam.twitch.embeddedads.bindings.EmbeddedAds
import app.reseam.twitch.settings.AdsSettings
import app.reseam.twitch.settings.TwitchStrings

val blockEmbeddedAds = patch(TwitchPatchStrings.blockEmbeddedAds) {
    description(TwitchPatchStrings.blockEmbeddedAdsDescription)
    compatibleWith(TWITCH)
    enabledByDefault(false)
    settings(twitchSettings, section(TwitchStrings.sectionAds, AdsSettings.blockEmbeddedAds, AdsSettings.embeddedAdsProxy, AdsSettings.customEmbeddedAdsProxy))

    execute {
        gate(AdsSettings.blockEmbeddedAds) {
            requestUrl.after { returnValue(EmbeddedAds.playlistUrl(result)) }
            requestHeaders.after { returnValue(EmbeddedAds.playlistHeaders(thisObject.field(requestUrlField), result)) }
            responseCreated.after { EmbeddedAds.onResponse(param(0), param(1)) }
            requestFailed.before { EmbeddedAds.onFailure(thisObject.field(callbackRequest).call(requestUrl)) }
        }
    }
}

// Native code calls the IVS network classes, so they keep their names.
private val ivsRequest = klass("com.amazonaws.ivs.net.Request")
private val ivsResponseCallback = klass($$"com.amazonaws.ivs.net.OkHttpClient$OkHttpResponseCallback")

private val requestUrl = ivsRequest.method("getUrl")
private val requestHeaders = ivsRequest.method("getHeaders")
private val requestUrlField = ivsRequest.field("url")
private val responseCreated = klass("com.amazonaws.ivs.net.Response").method("<init>") { params(Type.Int, Type.String) }
private val requestFailed = ivsResponseCallback.method("onFailure")
private val callbackRequest = ivsResponseCallback.field("request")
