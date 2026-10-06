// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtubemusic.layout

import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patch.settings.whenEnabled
import app.reseam.patches.youtubemusic.core.YOUTUBE_MUSIC
import app.reseam.patches.youtubemusic.core.YouTubeMusicSettings
import app.reseam.patches.youtubemusic.core.YouTubeMusicSettingsPages
import app.reseam.patches.youtubemusic.core.youTubeMusicSettings

// Preference keys from xml/settings_headers.
private val ROWS = mapOf(
    "pref_key_parent_tools" to YouTubeMusicSettings.hideSettingsFamilyCenter,
    "settings_header_data_saving" to YouTubeMusicSettings.hideSettingsDataSaving,
    "settings_header_downloads_and_storage" to YouTubeMusicSettings.hideSettingsDownloads,
    "settings_header_notifications" to YouTubeMusicSettings.hideSettingsNotifications,
    "settings_header_privacy_and_location" to YouTubeMusicSettings.hideSettingsPrivacy,
    "settings_header_recommendations" to YouTubeMusicSettings.hideSettingsRecommendations,
    "settings_header_about_youtube_music" to YouTubeMusicSettings.hideSettingsAbout,
)

val hideSettingsMenuItems = patch("Hide settings menu items") {
    description("Adds options to hide rows from YouTube Music's settings screen.")
    compatibleWith(YOUTUBE_MUSIC)
    settings(youTubeMusicSettings, section(YouTubeMusicSettingsPages.Interface, "Settings menu", *ROWS.values.toTypedArray()))

    execute {
        // Removed rather than hidden: the app shows some rows again once its settings load, and
        // it skips a row it cannot find.
        settingsScreen.after {
            val rows = thisObject.call(preferenceScreen)
            ROWS.forEach { (key, setting) -> whenEnabled(setting) { rows.call(removeRowByKey, string(key)) } }
        }
    }
}

private val settingsHeaders = klass("com.google.android.apps.youtube.music.settings.fragment.SettingsHeadersFragment")

private val settingsScreen = settingsHeaders.method("onCreatePreferences")

private val preferenceScreen = settingsHeaders.method("getPreferenceScreen", inherited = true) { params() }

// androidx removePreferenceRecursively: finds the row anywhere under the group by its key and
// removes it from its own parent. The other CharSequence method is findPreference.
private val removeRowByKey = method("removeRowByKey") {
    inClass(klass("androidx.preference.PreferenceGroup"))
    params(Type.CharSequence)
    returns(Type.Boolean)
}
