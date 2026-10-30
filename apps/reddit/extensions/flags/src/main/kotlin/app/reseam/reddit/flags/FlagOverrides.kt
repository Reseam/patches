// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("FlagOverrides")

package app.reseam.reddit.flags

import app.reseam.bundle.ReseamPatches
import app.reseam.runtime.Export
import app.reseam.runtime.Reseam
import java.util.regex.Pattern

@Volatile private var cachedOverrides: Overrides? = null
private val groupSeparator = Pattern.compile(";")
private val partSeparator = Pattern.compile("\\|")
private val comma = Pattern.compile(",")

@Export fun isDisabled(flag: String): Boolean = flag in overrides().disabled

@Export fun isForced(flag: String): Boolean = flag in overrides().forced

private fun overrides(): Overrides {
    cachedOverrides?.let {
        return it
    }
    val groups =
        groupSeparator.split(spec()).mapNotNull { group ->
            val parts = partSeparator.split(group, -1)
            if (Reseam.isEnabled(ReseamPatches.NAME, parts[0], parts[1].toBoolean())) {
                Overrides(flags(parts[2]), flags(parts[3]))
            } else {
                null
            }
        }
    val current =
        Overrides(groups.flatMap { it.disabled }.toSet(), groups.flatMap { it.forced }.toSet())
    // Flags are read before settings load; keep the defaults until they have.
    if (Reseam.isStarted) cachedOverrides = current
    return current
}

private fun flags(csv: String): Set<String> =
    if (csv.isEmpty()) emptySet() else comma.split(csv).toSet()

// Groups as "settingKey|default|disabled,flags|forced,flags;...", written in by the patch that owns
// the toggles.
@Export external fun spec(): String

private data class Overrides(val disabled: Set<String>, val forced: Set<String>)
