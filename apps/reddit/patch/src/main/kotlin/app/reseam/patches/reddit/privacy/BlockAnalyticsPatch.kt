// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.reddit.privacy

import app.reseam.patch.Type
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.reddit.core.REDDIT
import app.reseam.patches.reddit.core.RedditSettings
import app.reseam.patches.reddit.core.redditSettings

private const val TRACKING_LEVEL = "com.reddit.mmp.TrackingAndAttributionLevel"

val blockAnalytics = patch("Block analytics") {
    description("Stops Reddit's event and metric logging and turns off AppsFlyer tracking.")
    compatibleWith(REDDIT)
    settings(redditSettings, section("Privacy", RedditSettings.blockAnalytics, RedditSettings.blockAppsFlyer))

    execute {
        gate(RedditSettings.blockAnalytics) {
            eventLogger.alwaysReturn()
            metricLogger.alwaysReturn()
        }
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

private val metricLogger = logger("com.reddit.eventkit.MetricLoggerImpl\$send\$1")

// NONE is the branch that calls AppsFlyerLib.stop.
private val appsFlyerLevel = method("appsFlyerLevel") {
    calls { owner("com.appsflyer.AppsFlyerLib"); name("stop") }
    hasParam(TRACKING_LEVEL)
}
