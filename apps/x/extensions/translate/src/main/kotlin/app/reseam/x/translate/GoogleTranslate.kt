// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.x.translate

import app.reseam.bundle.send
import app.reseam.bundle.strings
import java.net.URLEncoder
import org.json.JSONArray
import org.json.JSONObject

/** Google Translate through its keyless web endpoint (`client=gtx`). */
internal object GoogleTranslate : TranslationService {
    private const val API = "https://translate.googleapis.com/translate_a"

    override val name = "Google Translate"
    override val maxTexts = 50
    override val maxBytes = 5_000

    // Google's codes for the languages engineLanguage names differently.
    private val googleCodes = mapOf("zh-Hans" to "zh-CN", "zh-Hant" to "zh-TW", "he" to "iw", "jv" to "jw")
    private val engineCodes = googleCodes.entries.associate { (engine, google) -> google to engine }

    private val languageList = LanguageList(name) {
        val lists = JSONObject(fetch("$API/l?client=gtx"))
        val sources = lists.getJSONObject("sl").keys().asSequence().toSet()
        lists.getJSONObject("tl").keys().asSequence().filter { it in sources }.map { engineCodes[it] ?: it }.toSet()
    }

    override fun languages() = languageList.get()

    override fun translate(from: String, to: String, texts: List<String>): List<String> {
        val body = texts.joinToString("&") { "q=" + URLEncoder.encode(it, "UTF-8") }
        val response = fetch("$API/t?client=gtx&sl=${google(from)}&tl=${google(to)}") {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            send(body.toByteArray())
        }
        return JSONArray(response).strings()
    }

    private fun google(language: String) = googleCodes[language] ?: language
}
