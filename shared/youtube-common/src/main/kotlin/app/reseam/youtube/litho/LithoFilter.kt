// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

@file:JvmName("LithoFilter")

package app.reseam.youtube.litho

import app.reseam.bundle.Logger
import app.reseam.runtime.Export
import app.reseam.youtube.core.debug
import app.reseam.youtube.litho.Filter.FilterContentType
import app.reseam.youtube.litho.FilterGroup.StringFilterGroup

private val filters = mutableListOf<Filter>()
// Multiple threads hide unrequested shelves and race filters' navigation-tab state.
private const val LAYOUT_THREAD_POOL_SIZE = 1
private val pathSearch = StringTrieSearch<Parameters>()
private val identifierSearch = StringTrieSearch<Parameters>()
private val searchIndex by lazy {
    filters += CustomFilter()
    filters.forEach { filter ->
        identifierSearch.register(filter, filter.identifierCallbacks, FilterContentType.IDENTIFIER)
        pathSearch.register(filter, filter.pathCallbacks, FilterContentType.PATH)
    }
    identifierSearch
}

@Export
fun register(filter: Filter) {
    filters += filter
}

private fun StringTrieSearch<Parameters>.register(
    filter: Filter,
    groups: List<StringFilterGroup>,
    type: FilterContentType,
) {
    groups.forEach { group ->
        group.filters.forEach { pattern ->
            addPattern(pattern) { _, start, _, parameter ->
                group.isEnabled && parameter.isFiltered(filter, group, type, start)
            }
        }
    }
}

@Export
fun setProtoBuffer(buffer: ByteArray?) {
    rememberProtoBuffer(buffer)
}

/** Returns the enclosing tree, to restore with exitTree when this tree finishes building. */
@Export fun enterTree(root: Any?, identifier: String?): Any? = enterBufferTree(root, identifier)

@Export
fun exitTree(outer: Any?) {
    exitBufferTree(outer)
}

// The patch serializes supported element kinds, returning null for unsupported ones.
@Export fun encodeElement(element: Any?): ByteArray? = null

@Export
fun isFiltered(
    identifier: String?,
    accessibilityId: String?,
    accessibilityText: String?,
    pathBuilder: StringBuilder?,
    element: Any?,
): Boolean {
    if (identifier.isNullOrEmpty() || pathBuilder == null || pathBuilder.isEmpty()) return false
    return try {
        val identifiers = searchIndex
        val accessibility =
            listOfNotNull(accessibilityId, accessibilityText)
                .filterNot { it.isBlank() }
                .joinToString("|")
        val parameters = Parameters(identifier, pathBuilder.toString(), accessibility, element)
        identifiers.matches(identifier, parameters) ||
            pathSearch.matches(parameters.path, parameters)
    } catch (ex: Exception) {
        Logger.error(ex) { "isFiltered failure" }
        false
    }
}

@Export
fun layoutThreadCount(original: Int): Int {
    if (original != LAYOUT_THREAD_POOL_SIZE)
        Logger.debug {
            "Overriding litho layout threads from: $original to: $LAYOUT_THREAD_POOL_SIZE"
        }
    return LAYOUT_THREAD_POOL_SIZE
}

private class Parameters(
    val identifier: String,
    val path: String,
    val accessibility: String,
    element: Any?,
) {
    private val buffer =
        lazy(LazyThreadSafetyMode.NONE) { withoutThemeResources(bufferFor(identifier)) }
    private val elementBuffer =
        lazy(LazyThreadSafetyMode.NONE) {
            encodeElement(element)?.let(::withoutThemeResources) ?: ByteArray(0)
        }

    fun isFiltered(
        filter: Filter,
        group: StringFilterGroup,
        type: FilterContentType,
        start: Int,
    ): Boolean =
        filter
            .isFiltered(
                LithoComponent(identifier, path, accessibility, buffer, elementBuffer, type, start),
                group,
            )
            .also {
                if (it)
                    Logger.debug {
                        "${filter.javaClass.simpleName} filtered ${if (type == FilterContentType.IDENTIFIER) "identifier: $identifier" else "path: $path"}"
                    }
            }
}
