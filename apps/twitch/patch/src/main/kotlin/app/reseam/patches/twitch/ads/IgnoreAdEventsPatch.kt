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
import app.reseam.patches.twitch.TwitchPatchStrings
import app.reseam.patches.twitch.core.TWITCH
import app.reseam.patches.twitch.core.twitchSettings
import app.reseam.twitch.adevents.bindings.AdEvents
import app.reseam.twitch.settings.AdsSettings
import app.reseam.twitch.settings.TwitchStrings

val ignoreAdEvents = patch(TwitchPatchStrings.ignoreAdEvents) {
    description(TwitchPatchStrings.ignoreAdEventsDescription)
    compatibleWith(TWITCH)
    settings(twitchSettings, section(TwitchStrings.sectionAds, AdsSettings.ignoreAdEvents))

    execute {
        gate(AdsSettings.ignoreAdEvents) {
            // The app registers these once at start, so changing the setting takes a restart.
            midrollRequestRegistration.skip()
            pixelTrackingRegistration.skip()
            commercialRegistration.skip()

            commercialMetadata.before {
                whenTrue(string("commercial").callVirtual("java.lang.String", "equals", "(Ljava/lang/Object;)Z", param(0))) { returnVoid() }
            }
            ivsMetadataHandler.before { whenTrue(AdEvents.isAdMetadata(param(0))) { returnVoid() } }
            reactNativeAdCueRoute.before { whenTrue(AdEvents.isAdCue(param(0))) { returnVoid() } }
        }
    }
}

// The app drops a PubSub message whose type has no registered subtype.
private val pubSubEventTypes = method("pubSubEventTypes") { strings("midroll_request", "pixel_tracking_update") }

private fun subtypeRegistration(label: String, type: String) = pubSubEventTypes
    .point(label) { string(type) }
    .next { invokeVirtual { params("Ljava/lang/Class;", Type.String); returns(Type.Void) } }

private val midrollRequestRegistration = subtypeRegistration("midrollRequestRegistration", "midroll_request")
private val pixelTrackingRegistration = subtypeRegistration("pixelTrackingRegistration", "pixel_tracking_update")
private val commercialRegistration = subtypeRegistration("commercialRegistration", "commercial")

private val commercialMetadata = method("commercialMetadata") { strings("commercial", "commercial_id", "ld_lat_data") }

private val ivsMetadataHandler = method("ivsMetadataHandler") {
    strings("twitch-stitched-ad", "twitch-maf-ad", "twitch-ad-quartile")
    params("Lorg/json/JSONObject;")
}

// React Native classes keep their names.
private val reactNativeAdCueRoute = klass("com.twitchrn.player.AdMetadataCueRouter").method("route")
