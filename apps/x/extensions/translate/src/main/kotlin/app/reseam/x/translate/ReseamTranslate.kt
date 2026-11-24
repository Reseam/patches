// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.x.translate

import app.reseam.bundle.send
import app.reseam.bundle.strings
import org.json.JSONArray
import org.json.JSONObject

internal object ReseamTranslate : TranslationService {
    private const val API = "https://api.reseam.app/v1/translate"

    override val name = "Reseam Translate"
    override val maxTexts = 50
    override val maxBytes = 18_000

    private val languageList = LanguageList(name) {
        val pairs = JSONObject(fetch("$API/languages")).getJSONArray("pairs")
        (0 until pairs.length()).flatMap { pairs.getJSONArray(it).strings() }.toSet()
    }

    override fun languages() = languageList.get()

    override fun translate(from: String, to: String, texts: List<String>): List<String> {
        val body = JSONObject().put("from", from).put("to", to).put("texts", JSONArray(texts))
        val response = fetch(API) {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            send(body.toString().toByteArray())
        }
        return JSONObject(response).getJSONArray("translations").strings()
    }
}
