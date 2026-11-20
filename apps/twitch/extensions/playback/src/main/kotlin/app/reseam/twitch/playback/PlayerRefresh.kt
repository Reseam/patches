// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("PlayerRefresh")

package app.reseam.twitch.playback

import android.view.View
import android.widget.ImageView
import app.reseam.runtime.Export
import app.reseam.twitch.settings.TwitchStrings

@Export
fun register(playerView: View?, presenter: Any?) = registerPlayer(playerView, presenter)

@Export
fun addButton(gear: View?, icon: Int) {
    if (gear !is ImageView) return
    val refresh = gearStyledButton(gear, icon, TwitchStrings.refreshStream)
    refresh.setOnClickListener { presenterFor(it)?.let(::reload) }
    addBeforeGear(gear, refresh)
}

/** Runs Twitch's Retry, which fetches a new access token and playlist for a live stream and reloads a video in place. */
@Export
external fun reload(presenter: Any?)
