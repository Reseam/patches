// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.bundle

import org.json.JSONArray
import org.json.JSONObject

fun JSONArray.objects(): List<JSONObject> = (0 until length()).mapNotNull { optJSONObject(it) }

fun JSONArray.strings(): List<String> = (0 until length()).map(::getString)

/** This object and every object nested in it, at any depth, in order, leaving out what sits under [skipping]. */
fun JSONObject.descendants(skipping: String? = null): Sequence<JSONObject> =
    sequenceOf(this) + keys().asSequence().filter { it != skipping }.flatMap { objectsIn(opt(it), skipping) }

private fun objectsIn(value: Any?, skipping: String?): Sequence<JSONObject> =
    when (value) {
        is JSONObject -> value.descendants(skipping)
        is JSONArray -> (0 until value.length()).asSequence().flatMap { objectsIn(value.opt(it), skipping) }
        else -> emptySequence()
    }

/** A JSON object of the fields that have a value; lists become JSON arrays. */
fun jsonObject(vararg fields: Pair<String, Any?>): JSONObject =
    JSONObject(fields.mapNotNull { (key, value) -> value?.let { key to it } }.toMap())
