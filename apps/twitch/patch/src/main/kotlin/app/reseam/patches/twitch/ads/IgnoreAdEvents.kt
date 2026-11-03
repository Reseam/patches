// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.twitch.ads

import app.reseam.patch.Type
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.twitch.core.TWITCH_PACKAGE
import app.reseam.patches.twitch.core.twitchSettings
import app.reseam.twitch.adevents.bindings.AdEvents
import app.reseam.twitch.settings.AdsSettings
import app.reseam.twitch.settings.TwitchStrings

// Gson builds each PubSub message as the subtype registered for its type, and the app drops a message whose type has none
val pubSubEventTypes = method("pubSubEventTypes") {
    strings("midroll_request", "pixel_tracking_update")
}

private fun subtypeRegistration(label: String, type: String) = pubSubEventTypes.point(label) { string(type) }.next {
    invokeVirtual { params("Ljava/lang/Class;", Type.String); returns(Type.Void) }
}

val midrollRequestRegistration = subtypeRegistration("midrollRequestRegistration", "midroll_request")
val pixelTrackingRegistration = subtypeRegistration("pixelTrackingRegistration", "pixel_tracking_update")
val commercialRegistration = subtypeRegistration("commercialRegistration", "commercial")

// The player metadata reports a commercial break with its length and id
val commercialMetadata = method("commercialMetadata") {
    strings("commercial", "commercial_id", "ld_lat_data")
}

// The IVS player sorts its HLS metadata by CLASS
val ivsMetadataHandler = method("ivsMetadataHandler") {
    strings("twitch-stitched-ad", "twitch-maf-ad", "twitch-ad-quartile")
    params("Lorg/json/JSONObject;")
}

// The React Native player's classes keep their names, and this one routes the same metadata to its ad state
val adMetadataCueRouter = klass("com.twitchrn.player.AdMetadataCueRouter")
val reactNativeAdCueRoute = adMetadataCueRouter.method("route")

@Suppress("unused")
val ignoreAdEvents = patch("Ignore ad events") {
    description("Ignores the ad breaks, picture-by-picture ads and ad tracking that Twitch announces to the app during a stream.")
    compatibleWith(TWITCH_PACKAGE)
    settings(twitchSettings, section(TwitchStrings.sectionAds, AdsSettings.ignoreAdEvents))

    execute {
        gate(AdsSettings.ignoreAdEvents) {
            // The app registers the subtypes once at start, so a change to the setting applies after a restart
            midrollRequestRegistration.skip()
            pixelTrackingRegistration.skip()
            commercialRegistration.skip()

            commercialMetadata.before {
                whenTrue(string("commercial").callVirtual("java.lang.String", "equals", "(Ljava/lang/Object;)Z", param(0))) { returnVoid() }
            }
            ivsMetadataHandler.before {
                whenTrue(AdEvents.isAdMetadata(param(0))) { returnVoid() }
            }
            reactNativeAdCueRoute.before {
                whenTrue(AdEvents.isAdCue(param(0))) { returnVoid() }
            }
        }
    }
}
