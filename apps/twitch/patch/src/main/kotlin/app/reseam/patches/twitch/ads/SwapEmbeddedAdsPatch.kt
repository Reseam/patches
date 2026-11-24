// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.twitch.ads

import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.twitch.TwitchPatchStrings
import app.reseam.patches.twitch.core.TWITCH
import app.reseam.patches.twitch.core.TwitchSettingsPages
import app.reseam.patches.twitch.core.ivsRequestUrl
import app.reseam.patches.twitch.core.twitchSettings
import app.reseam.twitch.embeddedads.bindings.VideoSwap
import app.reseam.twitch.settings.AdsSettings
import app.reseam.twitch.settings.TwitchStrings

val swapEmbeddedAds = patch(TwitchPatchStrings.swapEmbeddedAds) {
    description(TwitchPatchStrings.swapEmbeddedAdsDescription)
    compatibleWith(TWITCH)
    settings(
        twitchSettings,
        section(
            TwitchSettingsPages.Ads,
            TwitchStrings.sectionEmbeddedAds,
            AdsSettings.swapEmbeddedAds,
            AdsSettings.swapEmbeddedAdsClientId,
        ),
    )

    execute {
        gate(AdsSettings.swapEmbeddedAds) {
            bodyConsumerCreated.after { VideoSwap.onBody(param(1), param(0).call(ivsRequestUrl)) }
            bodyRead.before {
                val source = thisObject.call(bodySource)
                whenTrue(VideoSwap.reads(thisObject, source)) { returnValue(VideoSwap.writeTo(thisObject, source, param(0))) }
            }
        }
    }
}

// Native code calls the IVS network classes, so they keep their names.
private val bodyConsumerCreated = klass("com.amazonaws.ivs.net.ByteBufferConsumer").method("<init>") {
    params("Lcom/amazonaws/ivs/net/Request;", $$"Lcom/amazonaws/ivs/net/ByteBufferConsumer$Provider;")
}

// OkHttpResponseCallback declares one anonymous class, the body provider, so javac names it $1.
private val bodyProvider = klass($$"com.amazonaws.ivs.net.OkHttpClient$OkHttpResponseCallback$1")

private val bodyRead = bodyProvider.method("writeTo")
private val bodySource = bodyProvider.method("getSource")
