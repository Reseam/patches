// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("NavigationBar")

package app.reseam.instagram.navigation

import app.reseam.instagram.settings.NavigationSettings
import app.reseam.runtime.Export

// The first name is the tab Instagram builds when the server didn't send one.
private val tabNames =
    mapOf(
        "home" to listOf("FEED"),
        "reels" to listOf("CLIPS"),
        "messages" to listOf("DIRECT"),
        "search" to listOf("SEARCH"),
        "notifications" to listOf("NEWS"),
        "create" to listOf("CREATION", "SHARE"),
        "profile" to listOf("PROFILE"),
    )

// Reseam settings open from the profile, so a bar without it has no way back to them.
private const val PROFILE = "profile"

private val slots =
    listOf(NavigationSettings.firstTab, NavigationSettings.secondTab, NavigationSettings.thirdTab, NavigationSettings.fourthTab, NavigationSettings.fifthTab)

private var visibleTabs: List<Any> = emptyList()

@Export
fun arrange(tabs: List<Any>): List<Any> {
    val all = (tabs.firstOrNull() as? Enum<*>)?.declaringJavaClass?.enumConstants ?: return tabs
    val order = slots.map { it.value }.let { if (PROFILE in it) it else it + PROFILE }.distinct()
    return order
        .mapNotNull { tabNames[it] }
        .mapNotNull { names -> tabs.firstOrNull { (it as Enum<*>).name in names } ?: all.firstOrNull { it.name == names.first() } }
        .also { visibleTabs = it }
}

@Export
fun landingTab(tab: Any): Any = if (visibleTabs.isEmpty() || tab in visibleTabs) tab else visibleTabs.first()
