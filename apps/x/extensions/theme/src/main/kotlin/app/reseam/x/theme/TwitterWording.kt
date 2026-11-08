// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("TwitterWording")

package app.reseam.x.theme

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import android.util.SparseArray
import app.reseam.runtime.Export

/** The raw table of Twitter wording: string name, locale qualifier, escaped text per line. */
@Export external fun wordingResource(): Int

@Export fun wrap(context: Context?): Context? = if (context != null && twitter()) TwitterContext(context) else context

// X's locale override and AppCompat derive their contexts through createConfigurationContext, so those keep the wording.
private class TwitterContext(base: Context) : ContextWrapper(base) {
    private val wording by lazy { TwitterResources(base.resources) }

    override fun getResources(): Resources = wording

    override fun createConfigurationContext(overrideConfiguration: Configuration): Context =
        TwitterContext(super.createConfigurationContext(overrideConfiguration))
}

@Suppress("DEPRECATION")
private class TwitterResources(base: Resources) : Resources(base.assets, base.displayMetrics, base.configuration) {
    private val texts = SparseArray<CharSequence?>()
    private val wording: Map<String, Map<String, String>> by lazy {
        openRawResource(wordingResource()).bufferedReader().useLines { lines ->
            lines.map { it.split('\t', limit = 3) }.groupBy({ it[0] }, { it[1] to unescape(it[2]) }).mapValues { it.value.toMap() }
        }
    }
    private val qualifiers: List<String> by lazy {
        val locale = configuration.locales[0]
        listOf("${locale.language}-r${locale.country}", locale.language)
    }

    override fun getText(id: Int): CharSequence = twitterText(id) ?: super.getText(id)

    override fun getText(id: Int, def: CharSequence?): CharSequence? = twitterText(id) ?: super.getText(id, def)

    // A string Twitter's wording does not cover in this language keeps X's text.
    private fun twitterText(id: Int): CharSequence? {
        val cached = texts.indexOfKey(id)
        if (cached >= 0) return texts.valueAt(cached)
        val byQualifier = entryName(id)?.let(wording::get)
        return byQualifier?.let { qualifiers.firstNotNullOfOrNull(it::get) }.also { texts.put(id, it) }
    }

    private fun entryName(id: Int): String? =
        try {
            getResourceEntryName(id)
        } catch (_: NotFoundException) {
            null
        }
}

private fun unescape(text: String): String =
    Regex("""\\(.)""").replace(text) {
        when (val escaped = it.groupValues[1]) {
            "n" -> "\n"
            "t" -> "\t"
            else -> escaped
        }
    }
