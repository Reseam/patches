// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.twitch.ads

import app.reseam.patch.method
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.points
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.twitch.core.TWITCH_PACKAGE
import app.reseam.patches.twitch.core.twitchSettings
import app.reseam.patches.twitch.shared.cutOffServer
import app.reseam.patches.twitch.shared.forceTrue
import app.reseam.patches.twitch.shared.isStaff
import app.reseam.twitch.settings.AdsSettings
import app.reseam.twitch.settings.TwitchStrings

private const val AD_SERVER = "https://edge.ads.twitch.tv/"
private const val AD_VERIFICATION_SERVER = "https://fw.adsafeprotected.com/"
private const val GOOGLE_AD_SERVER = "https://pagead2.googlesyndication.com/"

// Applies Twitch's staff ad debug switches before a stream decides whether to request an ad
val adEligibilityOverride = method("adEligibilityOverride") {
    strings("skip_preroll", "decline_all_ads")
}

val adEligibilityStaffChecks = adEligibilityOverride.points("adEligibilityStaffChecks") { calls(isStaff) }

// The staff switch that declines every ad makes the stream skip its ad request
val declineAllAdsKey = adEligibilityOverride.point("declineAllAdsKey") { string("decline_all_ads") }
val declineAllAdsRead = declineAllAdsKey.next { invokeInterface { name("getBoolean") } }

// Feed, browse and remaining stream ad requests fail before they reach the ad server
val adServerUrls = methods("adServerUsers") { stringsStartingWith(AD_SERVER) }.points("adServerUrls") { stringContains(AD_SERVER) }

// Integral Ad Science checks that an ad was seen, and Google Mobile Ads reports its own pings
val adVerificationUrls = methods("adVerificationUsers") { stringsStartingWith(AD_VERIFICATION_SERVER) }.points("adVerificationUrls") { stringContains(AD_VERIFICATION_SERVER) }
val googleAdUrls = methods("googleAdUsers") { stringsStartingWith(GOOGLE_AD_SERVER) }.points("googleAdUrls") { stringContains(GOOGLE_AD_SERVER) }

@Suppress("unused")
val blockVideoAds = patch("Block video ads") {
    description("Blocks the video, audio and display ads the app requests itself and their verification pings, while ads stitched into the stream need Block embedded ads.")
    compatibleWith(TWITCH_PACKAGE)
    settings(twitchSettings, section(TwitchStrings.sectionAds, AdsSettings.blockVideoAds))

    execute {
        gate(AdsSettings.blockVideoAds) {
            val staffChecks = adEligibilityStaffChecks.all
            require(staffChecks.isNotEmpty()) { "The ad eligibility override no longer checks the staff flag" }
            staffChecks.forEach { staffCheck -> forceTrue(staffCheck) }
            forceTrue(declineAllAdsRead)

            val adUrls = cutOffServer(adServerUrls, AD_SERVER)
            val verificationUrls = cutOffServer(adVerificationUrls, AD_VERIFICATION_SERVER) + cutOffServer(googleAdUrls, GOOGLE_AD_SERVER)
            log.info("Declined ad eligibility at ${staffChecks.size} staff checks and the decline all ads switch, and cut off $adUrls ad server URLs and $verificationUrls verification URLs")
        }
    }
}
