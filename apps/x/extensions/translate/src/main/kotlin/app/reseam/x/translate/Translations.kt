// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.x.translate

import app.reseam.x.api.RemoteLookup
import java.io.IOException

internal data class TranslationKey(val from: String, val to: String, val text: String)

/** Translations from each service, cached and batched into as few requests as its limits allow. */
internal object Translations {
    private val lookups = Translator.entries.mapNotNull { it.service }.associateWith { service ->
        RemoteLookup(service.name, { keys -> requests(service, keys) }) { keys ->
            val pair = keys.first()
            val translations = service.translate(pair.from, pair.to, keys.map { it.text })
            if (translations.size != keys.size) throw IOException("Got ${translations.size} translations for ${keys.size} texts")
            keys.zip(translations).toMap()
        }
    }

    fun get(service: TranslationService, keys: Set<TranslationKey>): Map<TranslationKey, String> =
        lookups.getValue(service).get(keys)

    /** One request per language pair, within the service's limits on texts and bytes per request. */
    private fun requests(service: TranslationService, keys: List<TranslationKey>): List<List<TranslationKey>> =
        keys.groupBy { it.from to it.to }.values.flatMap { withinLimits(service, it) }

    private fun withinLimits(service: TranslationService, keys: List<TranslationKey>): List<List<TranslationKey>> {
        val requests = mutableListOf<MutableList<TranslationKey>>()
        var bytes = 0
        keys.forEach { key ->
            val size = key.text.toByteArray().size
            val current = requests.lastOrNull()?.takeIf { it.size < service.maxTexts && bytes + size <= service.maxBytes }
            if (current == null) requests += mutableListOf(key) else current += key
            bytes = if (current == null) size else bytes + size
        }
        return requests
    }
}
