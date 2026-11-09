// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("StartupTab")

package app.reseam.twitch.navigation

import android.app.Activity
import app.reseam.runtime.Export

@Export
fun openBrowse(activity: Activity, destinationKey: String, defaultDestination: Enum<*>, browseDestination: Enum<*>) {
    val intent = activity.intent
    if (intent.getIntExtra(destinationKey, defaultDestination.ordinal) == defaultDestination.ordinal) intent.putExtra(destinationKey, browseDestination.ordinal)
}
