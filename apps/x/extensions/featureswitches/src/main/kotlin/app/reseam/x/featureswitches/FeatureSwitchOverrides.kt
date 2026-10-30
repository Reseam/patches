// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("FeatureSwitchOverrides")

package app.reseam.x.featureswitches

import app.reseam.bundle.ReseamPatches
import app.reseam.bundle.parseOverrides
import app.reseam.runtime.Export
import app.reseam.runtime.ToggleValue
import app.reseam.x.settings.XSettings
import java.util.concurrent.ConcurrentHashMap

private val overrides = ConcurrentHashMap<String, ToggleValue>()
@Volatile private var userOverrides = emptyMap<String, Any>()

@Export
fun disable(key: String, toggleKey: String, toggleDefault: Boolean) {
    overrides[key] = ToggleValue(ReseamPatches.NAME, toggleKey, toggleDefault)
}

@Export
fun applyUserOverrides() {
    userOverrides = parseOverrides(XSettings.featureSwitchOverrides.value).mapValues { typed(it.value) }
}

/** Null keeps the app's own value. The user's overrides win over the ones patches set. */
@Export
fun lookup(key: String): Any? = userOverrides[key] ?: false.takeIf { overrides[key]?.value == true }

// The app's typed getters check the value's class and fall back to their default on a mismatch.
private fun typed(value: String): Any = when {
    value.equals("true", ignoreCase = true) -> true
    value.equals("false", ignoreCase = true) -> false
    value.startsWith('[') && value.endsWith(']') ->
        value.substring(1, value.length - 1).split(',').map { it.trim() }.filter { it.isNotEmpty() }
    value.length >= 2 && value.startsWith('"') && value.endsWith('"') -> value.substring(1, value.length - 1)
    else -> value.toLongOrNull() ?: value.toDoubleOrNull() ?: value
}
