// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("NavigationBar")

package app.reseam.x.navigation

import app.reseam.runtime.Export
import app.reseam.x.settings.XSettings

// X leaves these out of the bar but still has their icons and screens. Other tabs missing from
// the bar were turned off by X or by another patch, such as the Grok tab.
private val restorable = setOf("COMMUNITIES", "SPACES")

// X always opens on Home, so without its tab there is no way back to it.
private const val HOME = "HOME"

private val slots = listOf(XSettings.firstTab, XSettings.secondTab, XSettings.thirdTab, XSettings.fourthTab, XSettings.fifthTab)

/** The bar's tabs in the chosen order; [blank] is the tab data of a restored tab. */
@Export
fun arrange(tabs: LinkedHashMap<Enum<*>, Any>, blank: Any): LinkedHashMap<Enum<*>, Any> {
    val all = tabs.keys.firstOrNull()?.declaringJavaClass?.enumConstants ?: return tabs
    val chosen = slots.map { it.value.uppercase() }
    val arranged = LinkedHashMap<Enum<*>, Any>()
    for (name in if (HOME in chosen) chosen else listOf(HOME) + chosen) {
        val tab = all.firstOrNull { it.name == name } ?: continue
        val data = tabs[tab] ?: blank.takeIf { name in restorable } ?: continue
        arranged.putIfAbsent(tab, data)
    }
    return arranged
}
