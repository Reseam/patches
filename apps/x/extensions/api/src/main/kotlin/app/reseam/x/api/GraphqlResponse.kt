// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.x.api

import org.json.JSONObject

/** One GraphQL response from X, parsed on first use and shared by every rewriter. */
class GraphqlResponse(val operation: String, variables: String?, val body: String) {
    val variables: JSONObject? by lazy { variables?.let(::JSONObject) }
    val json: JSONObject by lazy { JSONObject(body) }
}

/** Changes X's GraphQL responses before the app parses them. Rewriters run in the order they register. */
interface GraphqlRewriter {
    val isEnabled: Boolean

    /** Edits [GraphqlResponse.json] in place and returns whether it changed anything. */
    fun rewrite(response: GraphqlResponse): Boolean
}
