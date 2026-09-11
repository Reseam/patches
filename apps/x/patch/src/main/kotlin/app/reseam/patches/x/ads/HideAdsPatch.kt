// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.x.ads

import app.reseam.patch.Type
import app.reseam.patch.appEntry
import app.reseam.patch.before
import app.reseam.patch.method
import app.reseam.patch.methodTarget
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.section
import app.reseam.patches.x.core.TimelineQueryFilter
import app.reseam.patches.x.core.X
import app.reseam.patches.x.core.XSettings
import app.reseam.patches.x.core.xSettings
import app.reseam.patches.x.featureswitches.disableFeatureSwitches
import app.reseam.patches.x.featureswitches.featureSwitchOverrides

private const val TIMELINE_VIEW_SCHEMA_QUERY = "SELECT name, sql FROM sqlite_master WHERE type = 'view' AND name = 'TimelineView'"

val hideAds = patch("Hide ads") {
    description("Removes promoted posts and Google ads from timelines.")
    compatibleWith(X)
    dependsOn(featureSwitchOverrides)
    settings(xSettings, section("Ads", XSettings.hideAds))

    execute {
        disableFeatureSwitches(XSettings.hideAds, "android_x_lite_ssp_ads_enabled", "ssp_ads_home_enabled")

        appEntry.before {
            call(TimelineQueryFilter.init, string(XSettings.hideAds.key), bool(XSettings.hideAds.default))
        }

        // Promoted posts reach the UI only through SQL on timeline_entry; rewriting statements in
        // every SQLiteConnection implementation survives Room regenerating the DAO text.
        val prepare = connectionPrepare
        val implementations = bytecode.classes
            .filter { prepare.owner in it.interfaces }
            .mapNotNull { it.method(prepare.name, prepare.proto) }
            .filter { it.instructionCount > 0 }
        require(implementations.isNotEmpty()) { "no implementation of ${prepare.descriptor} found" }
        for (implementation in implementations) {
            methodTarget("prepare:${implementation.owner}") { implementation }.before {
                param(0).assign(call(TimelineQueryFilter.rewrite, param(0)))
            }
        }
    }
}

// Room validates the view on open with this fixed statement, calling SQLiteConnection.prepare.
val timelineViewSchemaCheck = method("timelineViewSchemaCheck") { strings(TIMELINE_VIEW_SCHEMA_QUERY) }

val connectionPrepare = timelineViewSchemaCheck
    .point { string(TIMELINE_VIEW_SCHEMA_QUERY) }
    .next { invokeInterface { params(Type.String) } }
    .callee("connectionPrepare")
