// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.x.privacy

import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.dex.methodRef
import app.reseam.patch.settings.before
import app.reseam.patch.settings.section
import app.reseam.patches.x.core.X
import app.reseam.patches.x.core.XSettings
import app.reseam.patches.x.core.xSettings
import app.reseam.patches.x.featureswitches.disableFeatureSwitches
import app.reseam.patches.x.featureswitches.featureSwitchOverrides

val privacy = patch("Privacy") {
    description("Stops the installed-apps report used for ad targeting and turns off ad tracking switches.")
    compatibleWith(X)
    dependsOn(featureSwitchOverrides)
    settings(xSettings, section("Privacy", XSettings.blockInstalledAppsScan, XSettings.disableAdTracking))

    execute {
        installedAppsWork.before(XSettings.blockInstalledAppsScan) { returnValue(call(workerSuccess)) }

        disableFeatureSwitches(
            XSettings.disableAdTracking,
            "ad_tracking_enabled",
            "adid_reporting_enabled",
            "google_analytics_automatic_screen_reporting_enabled",
        )
    }
}

// doWork keeps its name: it overrides a WorkManager method.
val installedAppsWorker = klass("installedAppsWorker") { strings("com.anthropic.claude", "com.openai.chatgpt") }
val installedAppsWork = installedAppsWorker.method("doWork") { paramCount(1) }

// Result.success(): the zero-argument WorkManager factory on the worker's own success path.
val workerSuccess = installedAppsWork
    .point { invokeStatic { paramCount(0) }; where { methodRef?.definingClass?.startsWith("Landroidx/work/") == true } }
    .callee("workerSuccess")
