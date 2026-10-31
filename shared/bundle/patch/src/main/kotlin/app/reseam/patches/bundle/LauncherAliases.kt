// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.bundle

import app.reseam.patch.PatchRuntime
import app.reseam.patch.XmlElement

/** A launcher entry the app can switch to at runtime: an alias of its launcher activity. */
class LauncherAlias(
    /** The alias's class name, relative to the package (`.reseam_twitter`). */
    val name: String,
    val label: String,
    val icon: String,
    val enabled: Boolean,
)

/**
 * Moves the launcher entry of [entry] into [aliases] of [activity], so the app switches its launcher
 * name and icon at runtime with `selectLauncherAlias`. Every alias exists in every build: Android
 * keeps an alias's enabled state across updates, so dropping the selected one would leave the app
 * without a launcher entry. [movesFilter] picks which of the entry's intent filters each alias
 * carries too.
 */
fun PatchRuntime.addLauncherAliases(
    activity: String,
    entry: String,
    aliases: List<LauncherAlias>,
    movesFilter: (XmlElement) -> Boolean = { true },
) {
    manifest.edit {
        val original = findByAttribute("android:name", entry).singleOrNull() ?: error("the launcher entry $entry is missing")
        val shortcutResource = original.children
            .firstOrNull { it.tag == "meta-data" && it["android:name"] == "android.app.shortcuts" }
            ?.get("android:resource")

        for (alias in aliases) manifest.addActivityAlias(activity, alias.name, alias.enabled, alias.label)
        for (alias in aliases) {
            val element = findByAttribute("android:name", alias.name).single()
            element["android:icon"] = alias.icon
            element["android:exported"] = "true"
            original.children.filter { it.tag == "intent-filter" && movesFilter(it) }.forEach { element.appendChild(it.clone()) }
            shortcutResource?.let { resource ->
                element.appendChild(createElement("meta-data").apply {
                    this["android:name"] = "android.app.shortcuts"
                    this["android:resource"] = resource
                })
            }
        }

        // Remove MAIN from the original entry so Android exposes one launcher entry.
        original.children
            .firstOrNull { filter ->
                filter.tag == "intent-filter" &&
                    filter.children.any { it.tag == "action" && it["android:name"] == "android.intent.action.MAIN" }
            }
            ?.remove()
    }
}
