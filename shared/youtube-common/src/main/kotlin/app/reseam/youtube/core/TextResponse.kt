// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.core

import org.json.JSONObject

data class TextResponse(val status: Int, val body: String?) {
    fun json(): JSONObject = JSONObject(checkNotNull(body))
}
