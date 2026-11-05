// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.x.api

import org.json.JSONArray
import org.json.JSONObject

fun JSONArray.objects(): List<JSONObject> = (0 until length()).mapNotNull { optJSONObject(it) }

fun JSONArray.strings(): List<String> = (0 until length()).map(::getString)

/** This object and every object nested in it, at any depth. */
fun JSONObject.descendants(): Sequence<JSONObject> =
    sequenceOf(this) + keys().asSequence().flatMap { objectsIn(opt(it)) }

private fun objectsIn(value: Any?): Sequence<JSONObject> =
    when (value) {
        is JSONObject -> value.descendants()
        is JSONArray -> (0 until value.length()).asSequence().flatMap { objectsIn(value.opt(it)) }
        else -> emptySequence()
    }

/** A JSON object of the fields that have a value; lists become JSON arrays. */
fun jsonObject(vararg fields: Pair<String, Any?>): JSONObject =
    JSONObject(fields.mapNotNull { (key, value) -> value?.let { key to it } }.toMap())
