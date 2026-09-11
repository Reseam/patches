// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.x.grok

import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patch.settings.skipWhen
import app.reseam.patches.x.core.X
import app.reseam.patches.x.core.XSettings
import app.reseam.patches.x.core.xSettings
import app.reseam.patches.x.featureswitches.disableFeatureSwitches
import app.reseam.patches.x.featureswitches.featureSwitchOverrides

val grok = patch("Grok") {
    description("Hides the Grok tab, the Grok button on posts, and the drawer entry, and can turn off Grok translations.")
    compatibleWith(X)
    dependsOn(featureSwitchOverrides)
    settings(
        xSettings,
        section("Grok", XSettings.hideGrokTab, XSettings.hideGrokPostButton, XSettings.hideGrokDrawerEntry, XSettings.disableGrokTranslations),
    )

    execute {
        // The tab list keeps GROK only while this switch is on.
        disableFeatureSwitches(XSettings.hideGrokTab, "android_webview_grok_tab_enabled", "grok_android_tab_badge_enabled")

        // The header button is built only while this switch is on, before any per-post flag.
        disableFeatureSwitches(XSettings.hideGrokPostButton, "grok_android_analyze_timelines_enabled", "grok_android_analyze_ads_enabled")

        // The drawer entry is its own composable, located by the two labels it can show.
        val getGrok = resources.id("string", "drawer_get_grok") ?: error("string/drawer_get_grok missing")
        val openGrok = resources.id("string", "drawer_open_grok") ?: error("string/drawer_open_grok missing")
        method("drawerGrokEntry") { literals(getGrok.toLong(), openGrok.toLong()) }.skipWhen(XSettings.hideGrokDrawerEntry)

        disableFeatureSwitches(
            XSettings.disableGrokTranslations,
            "grok_translations_post_auto_translation_is_enabled",
            "grok_translations_community_note_translation_is_enabled",
        )
    }
}
