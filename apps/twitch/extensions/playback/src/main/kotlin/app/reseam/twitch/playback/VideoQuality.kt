// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("VideoQuality")

package app.reseam.twitch.playback

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import app.reseam.runtime.Export
import app.reseam.twitch.settings.PlaybackSettings
import com.amazonaws.ivs.player.Player
import java.util.WeakHashMap

private const val AUTO = "auto"
private const val SOURCE = "source"
private const val SAME_AS_WIFI = "same"

// Twitch names the top rendition after its size, such as 1080p60 (source).
private const val SOURCE_LABEL = "(source)"

// Twitch saves no quality under a suffix with this ending, so a lookup with one returns the default it is given.
private const val UNSAVED_ENDING = "_reseam_unsaved"

private const val PREFERENCES = "reseam_video_quality"
private const val AUTO_PICKED = "auto_picked"

private var connectivity: ConnectivityManager? = null
private var preferences: SharedPreferences? = null

// Holds the session each React Native player last started, so the start quality applies once per session and leaves a quality you pick later alone.
private val startedSessions = WeakHashMap<Any, String>()

@Export
fun init(application: Application) {
    connectivity = application.getSystemService(ConnectivityManager::class.java)
    // The file loads in the background from here, so the reads at playback start don't wait for the disk.
    preferences = application.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
}

/** Returns the chosen quality when [twitchDefault] is Twitch's Auto, and [twitchDefault] otherwise. */
@Export
fun initial(twitchDefault: String?): String? {
    if (twitchDefault != AUTO) return twitchDefault
    val mobileData = PlaybackSettings.mobileDataQuality.value
    if (mobileData != SAME_AS_WIFI && connectivity?.isActiveNetworkMetered == true) return mobileData
    return PlaybackSettings.defaultVideoQuality.value
}

/** Returns the saved quality's [suffix] while Remember video quality is on, or a suffix Twitch never saves a quality under. */
@Export
fun savedQualitySuffix(suffix: String?): String? = if (PlaybackSettings.rememberVideoQuality.value) suffix else suffix + UNSAVED_ENDING

/** Returns the stream's rendition for a chosen quality, or Auto when the stream has none at that quality. Any other name, and any name asked for before the stream loads, comes back as [requested]. */
@Export
fun rendition(requested: String?, player: Any?): String? {
    if (requested != SOURCE && requested?.toIntOrNull() == null) return requested
    // Before the stream loads, Twitch keeps the name and asks again once the player is ready.
    val qualities = (player as? Player)?.qualities.orEmpty().filterNotNull().ifEmpty { return requested }
    val candidates =
        if (requested == SOURCE) {
            qualities.filter { it.name.endsWith(SOURCE_LABEL, ignoreCase = true) }.ifEmpty { qualities }
        } else {
            qualities.filter { it.name.startsWith("${requested}p") }
        }
    return candidates.maxByOrNull { it.bitrate }?.name ?: AUTO
}

/** Switches a React Native [player] to the chosen quality once per [slot] and session, when the slot [ownsAdState] and no quality you picked applies. */
@Export
fun startQuality(slot: Any?, player: Any?, ownsAdState: Boolean) {
    val ivsPlayer = player as? Player ?: return
    val session = ivsPlayer.sessionId
    if (slot == null || !ownsAdState || session == null || startedSessions.put(slot, session) == session || !ivsPlayer.isAutoQualityMode || keptAutoPick()) return
    val name = rendition(initial(AUTO), ivsPlayer)
    val quality = ivsPlayer.qualities?.firstOrNull { it?.name == name } ?: return
    ivsPlayer.setAutoQualityMode(false)
    ivsPlayer.setQuality(quality)
}

/** Remembers whether your last pick in a main React Native player was Auto, since Twitch saves Auto as no choice. [auto] is true for a pick of Auto, and [ownsAdState] marks a main player. */
@Export
fun rememberPick(auto: Boolean, ownsAdState: Boolean) {
    if (ownsAdState && autoPicked() != auto) preferences?.edit()?.putBoolean(AUTO_PICKED, auto)?.apply()
}

private fun autoPicked() = preferences?.getBoolean(AUTO_PICKED, false) == true

// An Auto pick stays only while Remember video quality is on.
private fun keptAutoPick() = PlaybackSettings.rememberVideoQuality.value && autoPicked()
