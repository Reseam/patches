// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.litho

import app.reseam.bundle.Logger
import app.reseam.youtube.core.debug
import app.reseam.youtube.litho.FilterGroup.StringFilterGroup
import app.reseam.youtubecommon.settings.YouTubeCommonSettings

/** One rule per line: `^path#accessibility$buffer`, everything but the path optional. */
internal class CustomFilter : Filter() {
    init {
        val groups = parseCustomFilters().toTypedArray()
        if (groups.isNotEmpty())
            Logger.debug { "Using custom filters: ${groups.contentToString()}" }
        addPathCallbacks(*groups)
    }

    override fun isFiltered(component: LithoComponent, group: StringFilterGroup): Boolean =
        (group as CustomFilterGroup).matches(component)
}

private val syntax = Regex("""(\^?)([^#$]*)(?:#([^$]*))?(?:\$(.*))?""")

private fun parseCustomFilters(): Collection<CustomFilterGroup> {
    val expressions = YouTubeCommonSettings.customFilterStrings.value
    if (expressions.isBlank()) return emptyList()
    // Rules differing only in buffer text share a group, so each buffer is searched once.
    val groups = mutableMapOf<String, CustomFilterGroup>()
    expressions
        .split('\n')
        .asSequence()
        .filterNot { it.isBlank() }
        .mapNotNull(::parseExpression)
        .forEach { expression ->
            val group =
                groups.getOrPut(expression.key) {
                    CustomFilterGroup(expression.startsWith, expression.path)
                }
            expression.accessibility?.let(group::addAccessibilityString)
            expression.buffer?.let(group::addBufferString)
        }
    return groups.values
}

private data class FilterExpression(
    val startsWith: Boolean,
    val path: String,
    val accessibility: String?,
    val buffer: String?,
) {
    val isValid: Boolean
        get() = path.isNotBlank() && accessibility != "" && buffer != ""

    val key: String
        get() = buildString {
            if (startsWith) append('^')
            append(path)
            accessibility?.let { append('#').append(it) }
            if (buffer != null) append('$')
        }
}

private fun parseExpression(expression: String): FilterExpression? {
    val match = syntax.find(expression)
    val parsed = match?.let {
        val (prefix, path) = it.destructured
        FilterExpression(prefix.isNotEmpty(), path, it.groups[3]?.value, it.groups[4]?.value)
    }
    if (parsed?.isValid == true) return parsed
    Logger.error { "Invalid custom filter: $expression" }
    return null
}

private class CustomFilterGroup(private val startsWith: Boolean, path: String) :
    StringFilterGroup(YouTubeCommonSettings.customFilter, path) {
    private var accessibilitySearch: StringTrieSearch<Unit>? = null
    private var bufferSearch: ByteTrieSearch<Unit>? = null

    fun matches(component: LithoComponent): Boolean =
        when {
            startsWith && component.index != 0 -> false
            accessibilitySearch?.matches(component.accessibility) == false -> false
            else -> bufferSearch?.matches(component.buffer) != false
        }

    fun addAccessibilityString(value: String) {
        (accessibilitySearch ?: StringTrieSearch<Unit>().also { accessibilitySearch = it })
            .addPattern(value)
    }

    fun addBufferString(value: String) {
        (bufferSearch ?: ByteTrieSearch<Unit>().also { bufferSearch = it }).addPattern(
            value.encodeToByteArray()
        )
    }

    override fun toString(): String = buildString {
        append("CustomFilterGroup{path=")
        if (startsWith) append('^')
        append(filters[0])
        accessibilitySearch?.let { append(", accessibility=").append(it.patterns) }
        bufferSearch?.let { search ->
            append(", buffer=")
            append(search.patterns.joinToString("") { "${it.toString(Charsets.UTF_8)}|" })
        }
        append('}')
    }
}
