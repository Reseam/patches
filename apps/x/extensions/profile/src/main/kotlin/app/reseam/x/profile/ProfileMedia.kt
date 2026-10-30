// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("ProfileMedia")

package app.reseam.x.profile

import app.reseam.runtime.Export
import app.reseam.x.settings.XSettings

/** The chosen sub-tab when this tab groups it, else X's own. */
@Export
fun initialSubTab(subTabs: List<Enum<*>>, initial: Enum<*>): Enum<*> {
    val chosen = XSettings.defaultMediaTab.value
    return subTabs.firstOrNull { it.name.equals(chosen, ignoreCase = true) } ?: initial
}
