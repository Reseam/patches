// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("LoginPage")

package app.reseam.twitch.login

import app.reseam.runtime.Export
import org.json.JSONException
import org.json.JSONObject

// Twitch parses the login page's result strictly, so a field its class does not declare crashes the app.
@Export
fun declaredFieldsOnly(result: String?, declaredFields: String): String? {
    if (result == null) return null
    val fields = try {
        JSONObject(result)
    } catch (_: JSONException) {
        return result
    }
    val declared = JSONObject()
    for (field in declaredFields.split(',')) {
        if (fields.has(field)) declared.put(field, fields.get(field))
    }
    return declared.toString()
}
