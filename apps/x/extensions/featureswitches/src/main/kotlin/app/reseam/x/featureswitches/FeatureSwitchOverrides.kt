// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("FeatureSwitchOverrides")

package app.reseam.x.featureswitches

import app.reseam.bundle.ReseamPatches
import app.reseam.bundle.parseOverrides
import app.reseam.runtime.Export
import app.reseam.runtime.ToggleValue
import app.reseam.x.settings.XSettings
import java.util.concurrent.ConcurrentHashMap

private val overrides = ConcurrentHashMap<String, () -> Any?>()
@Volatile private var userOverrides = emptyMap<String, Any>()

/** Forces the switch [key] to [value], written as in the user overrides, while the toggle is on. */
@Export
fun force(key: String, value: String, toggleKey: String, toggleDefault: Boolean) {
    val toggle = ToggleValue(ReseamPatches.NAME, toggleKey, toggleDefault)
    val forced = typed(value)
    overrides[key] = { forced.takeIf { toggle.value } }
}

/** Forces the switch [key] off while [condition] holds. */
fun disableWhile(key: String, condition: () -> Boolean) {
    overrides[key] = { false.takeIf { condition() } }
}

@Export
fun applyUserOverrides() {
    userOverrides = parseOverrides(XSettings.featureSwitchOverrides.value).mapValues { typed(it.value) }
}

/** Null keeps the app's own value. The user's overrides win over the ones patches set. */
@Export
fun lookup(key: String): Any? = userOverrides[key] ?: overrides[key]?.invoke()

private fun typed(value: String): Any = when {
    value.equals("true", ignoreCase = true) -> true
    value.equals("false", ignoreCase = true) -> false
    value.startsWith('[') && value.endsWith(']') ->
        value.removeSurrounding("[", "]").split(',').map(String::trim).filter(String::isNotEmpty)
    value.length >= 2 && value.startsWith('"') && value.endsWith('"') -> value.removeSurrounding("\"")
    else -> value.toLongOrNull() ?: value.toDoubleOrNull() ?: value
}
