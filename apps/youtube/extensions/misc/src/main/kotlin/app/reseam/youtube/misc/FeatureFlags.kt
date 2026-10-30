// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("FeatureFlags")

package app.reseam.youtube.misc

import app.reseam.bundle.Logger
import app.reseam.bundle.parseOverrides
import app.reseam.runtime.Export
import app.reseam.youtube.core.debug
import app.reseam.youtube.settings.AdvancedSettings

@Export
fun booleanOverride(id: Long, original: Boolean): Boolean {
    val value = overrides[id]
    val override =
        when {
            value.equals("true", ignoreCase = true) -> true
            value.equals("false", ignoreCase = true) -> false
            else -> return original
        }
    Logger.debug { "Feature flag $id: $original -> $override" }
    return override
}

@Export
fun stringOverride(id: Long, original: String): String {
    val value = overrides[id] ?: return original
    Logger.debug { "Feature flag $id: '$original' -> '$value'" }
    return value
}

private val overrides by lazy {
    buildMap {
        for ((id, value) in parseOverrides(AdvancedSettings.featureFlagOverrides.value)) {
            when (val number = id.toLongOrNull()) {
                null -> Logger.error { "Ignoring feature flag override with a non-numeric id: $id" }
                else -> put(number, value)
            }
        }
    }
}
