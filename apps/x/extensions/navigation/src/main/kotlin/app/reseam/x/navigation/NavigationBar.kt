// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("NavigationBar")

package app.reseam.x.navigation

import app.reseam.runtime.Export
import app.reseam.x.settings.XSettings

// Dropped from the bar by X but still have a screen, so a blank tab entry brings them back.
private val restorable = setOf("COMMUNITIES", "SPACES")

// X always opens on Home, so a bar without it has no way back.
private const val HOME = "HOME"

private val slots = listOf(XSettings.firstTab, XSettings.secondTab, XSettings.thirdTab, XSettings.fourthTab, XSettings.fifthTab)

/** The bar's tabs in the chosen order; [blank] is the tab data of a restored tab. */
@Export
fun arrange(tabs: LinkedHashMap<Enum<*>, Any>, blank: Any): LinkedHashMap<Enum<*>, Any> {
    val all = tabs.keys.firstOrNull()?.declaringJavaClass?.enumConstants ?: return tabs
    val order = slots.map { it.value.uppercase() }.let { if (HOME in it) it else listOf(HOME) + it }
    return order.distinct()
        .mapNotNull { name -> all.firstOrNull { it.name == name } }
        .mapNotNull { tab -> (tabs[tab] ?: blank.takeIf { tab.name in restorable })?.let { tab to it } }
        .toMap(LinkedHashMap())
}
