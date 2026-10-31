// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("NavigationTabs")

package app.reseam.instagram.navigation

import app.reseam.runtime.Export

@Export
fun filter(tabs: List<Any>, hideReels: Boolean, hideCreate: Boolean): List<Any> {
    val hiddenTabs = mapOf("CLIPS" to hideReels, "CREATION" to hideCreate, "SHARE" to hideCreate)
    return tabs.filterNot { hiddenTabs[(it as Enum<*>).name] == true }
}
