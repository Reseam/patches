// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.hide

import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.section
import app.reseam.patch.settings.whenEnabled
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettings
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings

private const val PREFERENCE_SCREEN = "androidx.preference.PreferenceScreen"

// Preference keys from YouTube's settings XML. Premium has one row for members and one for offers.
private val ROWS = mapOf(
    "account_switcher_key" to YouTubeSettings.hideSettingsAccountSwitcher,
    "parent_tools_key" to YouTubeSettings.hideSettingsFamilyCenter,
    "premium_benefits_category_key" to YouTubeSettings.hideSettingsPremium,
    "premium_category_key" to YouTubeSettings.hideSettingsPremium,
    "subscription_product_setting_key" to YouTubeSettings.hideSettingsPurchases,
    "billing_and_payment_key" to YouTubeSettings.hideSettingsBilling,
    "badges_key" to YouTubeSettings.hideSettingsBadges,
    "your_data_key" to YouTubeSettings.hideSettingsYourData,
    "connected_accounts_browse_page_key" to YouTubeSettings.hideSettingsConnectedApps,
    "premium_early_access_browse_page_key" to YouTubeSettings.hideSettingsExperimentalFeatures,
    "pair_with_tv_key" to YouTubeSettings.hideSettingsWatchOnTv,
    "help_key" to YouTubeSettings.hideSettingsHelp,
    "send_feedback_key" to YouTubeSettings.hideSettingsSendFeedback,
)

val hideSettingsMenuItems = patch("Hide settings menu items") {
    description("Adds options to hide rows from YouTube's settings screen.")
    compatibleWith(YOUTUBE)
    settings(youTubeSettings, section(YouTubeSettingsPages.Appearance, "Settings menu", *ROWS.values.distinct().toTypedArray()))

    execute {
        val screen = removeServerHiddenRows
            .point("settingsScreen") { invokeVirtual { returns(PREFERENCE_SCREEN); params() } }
            .callee()
        // Runs after every rebuild of the screen, once YouTube has dropped the rows it hides itself.
        removeServerHiddenRows.after {
            val rows = thisObject.call(screen)
            ROWS.forEach { (key, toggle) ->
                whenEnabled(toggle) { rows.call(removeRowByKey, string(key)) }
            }
        }
    }
}

// Finds the row anywhere under the group by its key and removes it from its own parent.
private val removeRowByKey = method("removeRowByKey") {
    inClass(klass("androidx.preference.PreferenceGroup"))
    params(Type.CharSequence)
    returns(Type.Void)
}

private val removeServerHiddenRows = method("removeServerHiddenRows") {
    calls(removeRowByKey)
    params(Type.List)
    returns(Type.Void)
}
