// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.x.grok

import app.reseam.patch.Type
import app.reseam.patch.dex.Opcode
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.x.core.X
import app.reseam.patches.x.core.XSettings
import app.reseam.patches.x.core.xSettings
import app.reseam.patches.x.featureswitches.disableFeatureSwitches
import app.reseam.patches.x.featureswitches.featureSwitchOverrides

val grok = patch("Grok") {
    description("Hides the Grok tab, the Grok buttons on posts and profiles, Imagine prompts, Grok in the composer and the drawer, and can turn off Grok translations.")
    compatibleWith(X)
    dependsOn(featureSwitchOverrides)
    settings(
        xSettings,
        section(
            "Grok",
            XSettings.hideGrokTab,
            XSettings.hideGrokPostButton,
            XSettings.hideGrokPrompts,
            XSettings.hideGrokImageTools,
            XSettings.hideGrokComposer,
            XSettings.hideGrokDrawerEntry,
            XSettings.disableGrokTranslations,
        ),
    )

    execute {
        // The tab list keeps GROK only while this switch is on.
        disableFeatureSwitches(XSettings.hideGrokTab, "android_webview_grok_tab_enabled", "grok_android_tab_badge_enabled")

        // The header button is built only while this switch is on, before any per-post flag.
        disableFeatureSwitches(XSettings.hideGrokPostButton, "grok_android_analyze_timelines_enabled", "grok_android_analyze_ads_enabled")
        // The profile header leaves out any menu item that is null. The constructor taking a defaults mask delegates here.
        gate(XSettings.hideGrokPostButton) {
            profileMenuItems.method("<init>") { custom { Type.Int !in parameterTypes } }.after {
                thisObject.set(askGrokItem, nullObject)
            }
        }

        // The drawer entry is its own composable, located by the two labels it can show.
        val getGrok = resources.id("string", "drawer_get_grok") ?: error("string/drawer_get_grok missing")
        val openGrok = resources.id("string", "drawer_open_grok") ?: error("string/drawer_open_grok missing")
        gate(XSettings.hideGrokDrawerEntry) {
            method("drawerGrokEntry") { literals(getGrok.toLong(), openGrok.toLong()) }.alwaysReturn()
        }
        disableFeatureSwitches(XSettings.hideGrokDrawerEntry, "android_grok_bot_sidebar_enabled")

        disableFeatureSwitches(
            XSettings.hideGrokPrompts,
            "grok_android_download_grok_cta_enabled",
            "grok_android_imagine_cta_enabled",
            "grok_android_imagine_cta_profile_enabled",
            "grok_android_imagine_create_your_own_enabled",
        )
        disableFeatureSwitches(XSettings.hideGrokImageTools, "grok_android_imagine_make_video", "grok_android_imagine_edit_image")
        disableFeatureSwitches(XSettings.hideGrokComposer, "grok_mobile_post_composer_is_enabled")

        disableFeatureSwitches(
            XSettings.disableGrokTranslations,
            "grok_translations_post_auto_translation_is_enabled",
            "grok_translations_community_note_translation_is_enabled",
            "grok_translations_community_note_auto_translation_is_enabled",
            "grok_translations_bio_translation_is_enabled",
            "grok_translations_notification_auto_translation_is_enabled",
            "grok_translations_poll_translation_is_enabled",
        )
    }
}

private const val PROFILE_MENU_ITEMS = "ProfileMenuItems(askGrokItem="

private val profileMenuItems = klass("profileMenuItems") { strings(PROFILE_MENU_ITEMS) }

private val askGrokItem = profileMenuItems.method("toString")
    .point { string(PROFILE_MENU_ITEMS) }
    .next { opcode(Opcode.IGET_OBJECT) }
    .field("askGrokItem")
