// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("FeedUnits")

package app.reseam.instagram.feed

import app.reseam.bundle.ReseamPatches
import app.reseam.runtime.Export
import app.reseam.runtime.ToggleValue
import java.util.concurrent.ConcurrentHashMap

// Read on every match, so a changed toggle applies to the next feed load.
private val hidden = ConcurrentHashMap<String, ToggleValue>()

@Export
fun hide(unitKey: String, toggleKey: String, toggleDefault: Boolean) {
    hidden[unitKey] = ToggleValue(ReseamPatches.NAME, toggleKey, toggleDefault)
}

@Export
fun matches(field: String, unitKey: Any?): Boolean =
    field == unitKey && hidden[field]?.value != true
