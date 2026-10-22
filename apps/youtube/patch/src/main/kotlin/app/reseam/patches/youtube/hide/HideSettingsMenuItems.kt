// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.hide

import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.section
import app.reseam.patch.settings.whenEnabled
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.youtube.settings.AppearanceSettings
import app.reseam.youtube.settings.YouTubeStrings

private const val PREFERENCE_SCREEN = "androidx.preference.PreferenceScreen"
private const val PREFERENCE_GROUP = "androidx.preference.PreferenceGroup"
private const val PREFERENCE = "androidx.preference.Preference"

// Preference keys from YouTube's settings XML. Premium has one row for members and one for offers.
private val ROWS = mapOf(
    "account_switcher_key" to AppearanceSettings.hideSettingsAccountSwitcher,
    "parent_tools_key" to AppearanceSettings.hideSettingsFamilyCenter,
    "premium_benefits_category_key" to AppearanceSettings.hideSettingsPremium,
    "premium_category_key" to AppearanceSettings.hideSettingsPremium,
    "subscription_product_setting_key" to AppearanceSettings.hideSettingsPurchases,
    "billing_and_payment_key" to AppearanceSettings.hideSettingsBilling,
    "badges_key" to AppearanceSettings.hideSettingsBadges,
    "your_data_key" to AppearanceSettings.hideSettingsYourData,
    "connected_accounts_browse_page_key" to AppearanceSettings.hideSettingsConnectedApps,
    "premium_early_access_browse_page_key" to AppearanceSettings.hideSettingsExperimentalFeatures,
    "pair_with_tv_key" to AppearanceSettings.hideSettingsWatchOnTv,
    "help_key" to AppearanceSettings.hideSettingsHelp,
    "send_feedback_key" to AppearanceSettings.hideSettingsSendFeedback,
)

val hideSettingsMenuItems = patch("Hide settings menu items") {
    description("Adds options to hide rows from YouTube's settings screen.")
    compatibleWith(YOUTUBE)
    settings(youTubeSettings, section(YouTubeSettingsPages.Appearance, YouTubeStrings.sectionSettingsMenu, *ROWS.values.distinct().toTypedArray()))

    execute {
        val screenRead = rebuildSettings
            .point("settingsScreen") { invokeVirtual { returns(PREFERENCE_SCREEN); params() } }
        val screen = screenRead.callee()
        val fragment = screenRead.writer(0).field()
        // Runs after every rebuild of the screen, once YouTube has dropped the rows it hides itself.
        rebuildSettings.after {
            val rows = thisObject.field(fragment).call(screen)
            whenNotNull(rows) {
                ROWS.forEach { (key, toggle) ->
                    whenEnabled(toggle) {
                        val row = rows.call(findRowByKey, string(key))
                        whenNotNull(row) { row.fieldOfType(PREFERENCE_GROUP).call(removeRow, row) }
                    }
                }
            }
        }
    }
}

// Older releases inline removal by key, but keep the recursive lookup and parent removal methods.
private val findRowByKey = method("findRowByKey") {
    inClass(klass(PREFERENCE_GROUP))
    params(Type.CharSequence)
    returns(PREFERENCE)
}

private val removeRow = method("removeRow") {
    inClass(klass(PREFERENCE_GROUP))
    flags(AccessFlags.PUBLIC)
    params(PREFERENCE)
    returns(Type.Void)
    calls(removeRowInternal)
}

private val removeRowInternal = method("removeRowInternal") {
    inClass(klass(PREFERENCE_GROUP))
    params(PREFERENCE)
    returns(Type.Void)
    calls { owner("java.util.List"); name("remove"); params(Type.Object) }
}

// The settings rebuild restores the requested fragment after applying the server's hidden rows.
private val rebuildSettings = method("rebuildSettings") {
    strings(":android:show_fragment")
    name("run")
    params()
    returns(Type.Void)
    calls { returns(PREFERENCE_SCREEN); params() }
}
