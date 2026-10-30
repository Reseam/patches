// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("SettingsRows")

package app.reseam.x.settings

import app.reseam.runtime.Export

@Export fun single(item: Any): List<Any> = listOf(item)

@Export fun pair(first: Any, second: Any): List<Any> = listOf(first, second)

/** Prepends [section] to the settings list once per [tag], surviving the screen's state copies. */
@Export
fun withSection(sections: List<Any>, tag: String, section: Any): MutableList<Any> {
    val list = sections as? ReseamSections ?: ReseamSections(sections)
    if (list.tags.add(tag)) list.add(0, section)
    return list
}

private class ReseamSections(sections: List<Any>) : ArrayList<Any>(sections) {
    val tags = HashSet<String>()
}
