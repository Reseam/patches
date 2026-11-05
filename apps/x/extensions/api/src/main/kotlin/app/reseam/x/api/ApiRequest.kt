// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.x.api

import org.json.JSONObject

/** A request to one of X's API endpoints, its body read on first use. */
class ApiRequest(val path: String, body: () -> ByteArray?) {
    val json: JSONObject? by lazy { body()?.let { JSONObject(String(it)) } }
}

/** Answers requests to one endpoint in place of X. */
interface ApiResponder {
    val path: String
    val isEnabled: Boolean

    /** The JSON body to answer with, or null to let the request go to X. */
    fun respond(request: ApiRequest): String?
}
