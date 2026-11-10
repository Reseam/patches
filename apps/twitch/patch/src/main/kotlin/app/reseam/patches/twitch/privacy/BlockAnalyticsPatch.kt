// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.twitch.privacy

import app.reseam.patch.Type
import app.reseam.patch.klass
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
import app.reseam.patches.twitch.core.twitchSettings
import app.reseam.twitch.settings.PrivacySettings
import app.reseam.twitch.settings.TwitchStrings

private const val BRANCH_API = "https://api2.branch.io/"
private const val BRANCH_CDN = "https://cdn.branch.io/"

val blockAnalytics = patch(TwitchPatchStrings.blockAnalytics) {
    description(TwitchPatchStrings.blockAnalyticsDescription)
    compatibleWith(TWITCH)
    settings(twitchSettings, section(TwitchStrings.sectionPrivacy, PrivacySettings.blockAnalytics))

    execute {
        gate(PrivacySettings.blockAnalytics) {
            comScoreStart.alwaysReturn()
            sentryEnabled.alwaysReturn(false)
            crashlyticsCollectionSwitch.alwaysReturn(false)

            val configureOverloads = sprigConfigureOverloads.all
            require(configureOverloads.isNotEmpty()) { "The app no longer configures Sprig" }
            configureOverloads.forEach { it.alwaysReturn() }

            nielsenPing.alwaysReturn()

            val branchUrls = cutOffServer(branchApiUrls, BRANCH_API) + cutOffServer(branchCdnUrls, BRANCH_CDN)
            log.info("Blocked comScore, Nielsen, Sentry, Crashlytics, Sprig and $branchUrls Branch URLs")
        }
    }
}

// R8 keeps the names of these public SDK classes.
private val comScoreStart = klass("com.comscore.Analytics").method("start") { params(Type.Context) }

// Sentry skips its whole setup, native code included, while its options report it disabled.
private val sentryEnabled = klass("io.sentry.SentryOptions").method("isEnabled") { paramCount(0) }

// Crashlytics and Firebase Sessions keep their reports on the device while this switch reads off.
private val crashlyticsCollectionSwitch = klass("com.google.firebase.crashlytics.FirebaseCrashlytics")
    .method("isCrashlyticsCollectionEnabled")
    .point("crashlyticsCollectionSwitch") { invokeVirtual { returns(Type.Boolean) } }
    .callee()

private val sprigConfigureOverloads = klass("com.userleap.UserLeap").methods("sprigConfigureOverloads") { name("configure") }

// Twitch's R8 rules keep the names of its Retrofit services.
private val nielsenPing = method("nielsenPing") {
    calls { owner($$"tv.twitch.android.shared.nielsen.NielsenS2SApi$NielsenS2SService"); name("firePing") }
}

private val branchApiUrls = methods("branchApiUsers") { stringsStartingWith(BRANCH_API) }
    .points("branchApiUrls") { stringContains(BRANCH_API) }

private val branchCdnUrls = methods("branchCdnUsers") { stringsStartingWith(BRANCH_CDN) }
    .points("branchCdnUrls") { stringContains(BRANCH_CDN) }
