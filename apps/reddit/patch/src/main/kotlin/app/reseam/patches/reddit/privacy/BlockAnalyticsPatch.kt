// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.reddit.privacy

import app.reseam.patch.Type
import app.reseam.patch.before
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.valueOf
import app.reseam.patch.settings.section
import app.reseam.patches.reddit.RedditPatchStrings
import app.reseam.patches.reddit.core.REDDIT
import app.reseam.reddit.settings.RedditSettings
import app.reseam.patches.reddit.core.redditSettings
import app.reseam.reddit.settings.RedditStrings

private const val TRACKING_LEVEL = "com.reddit.mmp.TrackingAndAttributionLevel"

val blockAnalytics = patch(RedditPatchStrings.blockAnalytics) {
    description(RedditPatchStrings.blockAnalyticsDescription)
    compatibleWith(REDDIT)
    settings(
        redditSettings,
        section(RedditStrings.sectionPrivacy, RedditSettings.blockAnalytics, RedditSettings.blockPostViews, RedditSettings.blockAppsFlyer),
    )

    execute {
        // Reddit's server skips posts it has view events for when it builds Home.
        eventLogger.before {
            whenInstanceOf(param(0), postViewEvent.descriptor) {
                whenTrue(valueOf(RedditSettings.blockPostViews)) { returnVoid() }
            } otherwise {
                whenTrue(valueOf(RedditSettings.blockAnalytics)) { returnVoid() }
            }
        }
        gate(RedditSettings.blockAnalytics) { metricLogger.alwaysReturn() }
        gate(RedditSettings.blockAppsFlyer) {
            appsFlyerLevel.before { param(0).assign(enumValue(TRACKING_LEVEL, "NONE")) }
        }
    }
}

private fun logger(sendLambda: String) = method(sendLambda) {
    calls(klass(sendLambda).method("<init>"))
    returns(Type.Void)
}

private val eventLogger = logger("com.reddit.eventkit.EventLoggerImpl\$send\$1")

private val postViewEvent = klass("postViewEvent") { strings("PostViewPost(correlationId=") }

private val metricLogger = logger("com.reddit.eventkit.MetricLoggerImpl\$send\$1")

// NONE is the branch that calls AppsFlyerLib.stop.
private val appsFlyerLevel = method("appsFlyerLevel") {
    calls { owner("com.appsflyer.AppsFlyerLib"); name("stop") }
    hasParam(TRACKING_LEVEL)
}
