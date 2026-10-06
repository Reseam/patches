// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.reddit.core

import app.reseam.patch.ExtMethod
import app.reseam.patch.MethodTarget
import app.reseam.patch.Type
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.ToggleSetting

// The startup store persists what these getters return, so an override also reaches it on the next launch.
private val dynamicConfig = klass("dynamicConfig") { strings("Illegal DynamicVariableType (NONE) cannot be converted to a DynamicType") }

private fun dynamicConfigGetter(type: String) = method("dynamicConfig $type") {
    inClass(dynamicConfig)
    params(Type.String)
    returns(type)
}

private val intConfig = dynamicConfigGetter("java.lang.Integer")
private val stringConfig = dynamicConfigGetter(Type.String)

fun overrideIntConfigs(setting: ToggleSetting, values: Map<String, Int>) = intConfig.override(setting, DynamicConfigOverrides.intValue, values)

fun overrideStringConfigs(setting: ToggleSetting, values: Map<String, String>) = stringConfig.override(setting, DynamicConfigOverrides.stringValue, values)

private fun MethodTarget.override(setting: ToggleSetting, lookup: ExtMethod, values: Map<String, Any>) = gate(setting) {
    before {
        val value = call(lookup, param(0), string(values.entries.joinToString(";") { (name, value) -> "$name=$value" }))
        whenNotNull(value) { returnValue(value) }
    }
}
