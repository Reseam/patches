// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.x.withheld

import org.json.JSONArray
import org.json.JSONObject

internal fun JSONArray.objects(): List<JSONObject> = (0 until length()).mapNotNull { optJSONObject(it) }

/** This object and every object nested in it, at any depth. */
internal fun JSONObject.descendants(): Sequence<JSONObject> =
    sequenceOf(this) + keys().asSequence().flatMap { objectsIn(opt(it)) }

private fun objectsIn(value: Any?): Sequence<JSONObject> =
    when (value) {
        is JSONObject -> value.descendants()
        is JSONArray -> (0 until value.length()).asSequence().flatMap { objectsIn(value.opt(it)) }
        else -> emptySequence()
    }
