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
import app.reseam.patches.twitch.TwitchPatchStrings
import app.reseam.patches.twitch.core.TWITCH
import app.reseam.patches.twitch.core.cutOffServer
import app.reseam.patches.twitch.core.forceTrue
import app.reseam.patches.twitch.core.isStaff
import app.reseam.patches.twitch.core.twitchSettings
import app.reseam.twitch.settings.AdsSettings
import app.reseam.twitch.settings.TwitchStrings

private const val AD_SERVER = "https://edge.ads.twitch.tv/"
private const val AD_VERIFICATION_SERVER = "https://fw.adsafeprotected.com/"
private const val GOOGLE_AD_SERVER = "https://pagead2.googlesyndication.com/"

val blockVideoAds = patch(TwitchPatchStrings.blockVideoAds) {
    description(TwitchPatchStrings.blockVideoAdsDescription)
    compatibleWith(TWITCH)
    settings(twitchSettings, section(TwitchStrings.sectionAds, AdsSettings.blockVideoAds))

    execute {
        gate(AdsSettings.blockVideoAds) {
            val staffChecks = adEligibilityStaffChecks.all
            require(staffChecks.isNotEmpty()) { "The ad eligibility override no longer checks the staff flag" }
            staffChecks.forEach { forceTrue(it) }
            forceTrue(declineAllAdsRead)

            val adUrls = cutOffServer(adServerUrls, AD_SERVER)
            val verificationUrls = cutOffServer(adVerificationUrls, AD_VERIFICATION_SERVER) + cutOffServer(googleAdUrls, GOOGLE_AD_SERVER)
            log.info("Declined ads at ${staffChecks.size} staff checks, and cut off $adUrls ad server and $verificationUrls verification URLs")
        }
    }
}

// Staff ad debug switches, applied before a stream decides whether to request an ad. Declining every ad skips the request.
private val adEligibilityOverride = method("adEligibilityOverride") { strings("skip_preroll", "decline_all_ads") }

private val adEligibilityStaffChecks = adEligibilityOverride.points("adEligibilityStaffChecks") { calls(isStaff) }

private val declineAllAdsRead = adEligibilityOverride
    .point("declineAllAdsKey") { string("decline_all_ads") }
    .next { invokeInterface { name("getBoolean") } }

private val adServerUrls = methods("adServerUsers") { stringsStartingWith(AD_SERVER) }
    .points("adServerUrls") { stringContains(AD_SERVER) }

private val adVerificationUrls = methods("adVerificationUsers") { stringsStartingWith(AD_VERIFICATION_SERVER) }
    .points("adVerificationUrls") { stringContains(AD_VERIFICATION_SERVER) }

private val googleAdUrls = methods("googleAdUsers") { stringsStartingWith(GOOGLE_AD_SERVER) }
    .points("googleAdUrls") { stringContains(GOOGLE_AD_SERVER) }
