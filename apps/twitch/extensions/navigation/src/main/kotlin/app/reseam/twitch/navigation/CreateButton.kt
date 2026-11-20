// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("CreateButton")

package app.reseam.twitch.navigation

import android.app.Activity
import android.view.Menu
import android.view.View
import app.reseam.runtime.Export

@Export
fun hideView(activity: Activity, id: Int) {
    activity.window.peekDecorView()?.findViewById<View>(id)?.visibility = View.GONE
}

// Hiding the menu item lets the other tabs share its space.
@Export
fun hideSlot(activity: Activity, bar: Int, slot: Int) {
    val bottomBar = activity.window.peekDecorView()?.findViewById<View>(bar) ?: return
    menuOf(bottomBar)?.findItem(slot)?.isVisible = false
}

@Export
external fun menuOf(bottomBar: View): Menu?
