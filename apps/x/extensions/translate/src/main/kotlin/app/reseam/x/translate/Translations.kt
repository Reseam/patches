// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.x.translate

import app.reseam.x.api.RemoteLookup

internal data class TranslationKey(val from: String, val to: String, val text: String)

/** Translations from Reseam Translate, cached and batched into as few requests as its limits allow. */
internal object Translations {
    private const val MAX_TEXTS = 50
    private const val MAX_BYTES = 18_000

    private val lookup =
        RemoteLookup("Reseam Translate", ::requests) { keys ->
            val pair = keys.first()
            keys.zip(ReseamTranslate.translate(pair.from, pair.to, keys.map { it.text })).toMap()
        }

    /** The languages translations exist between, or null while that list is unavailable. */
    fun languages(): Set<String>? = ReseamTranslate.languages()

    fun get(keys: Set<TranslationKey>): Map<TranslationKey, String> = lookup.get(keys)

    /** One request per language pair, within the engine's limits on texts and bytes per request. */
    private fun requests(keys: List<TranslationKey>): List<List<TranslationKey>> =
        keys.groupBy { it.from to it.to }.values.flatMap(::withinLimits)

    private fun withinLimits(keys: List<TranslationKey>): List<List<TranslationKey>> {
        val requests = mutableListOf<MutableList<TranslationKey>>()
        var bytes = 0
        keys.forEach { key ->
            val size = key.text.toByteArray().size
            val current = requests.lastOrNull()?.takeIf { it.size < MAX_TEXTS && bytes + size <= MAX_BYTES }
            if (current == null) requests += mutableListOf(key) else current += key
            bytes = if (current == null) size else bytes + size
        }
        return requests
    }
}
