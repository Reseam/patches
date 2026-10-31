// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.litho

import app.reseam.youtube.litho.FilterGroup.ByteArrayFilterGroup
import app.reseam.youtube.litho.FilterGroup.FilterGroupResult
import app.reseam.youtube.litho.FilterGroup.StringFilterGroup

abstract class FilterGroupList<V, T : FilterGroup<V>>
protected constructor(private val search: TrieSearch<V, FilterGroupResult>) : Iterable<T> {
    private val filterGroups = mutableListOf<T>()

    fun addAll(vararg groups: T) {
        filterGroups += groups
        groups.forEach { group ->
            group.filters.forEach { pattern ->
                search.addPattern(pattern) { _, start, length, parameter ->
                    if (group.isEnabled) {
                        parameter.setValues(start, length)
                        true
                    } else false
                }
            }
        }
    }

    override fun iterator(): Iterator<T> = filterGroups.iterator()

    fun check(stack: V?): FilterGroupResult = FilterGroupResult().also { search.matches(stack, it) }

    class StringFilterGroupList : FilterGroupList<String, StringFilterGroup>(StringTrieSearch())

    /** For a single byte pattern KMP in ByteArrayFilterGroup is slightly faster. */
    class ByteArrayFilterGroupList :
        FilterGroupList<ByteArray, ByteArrayFilterGroup>(ByteTrieSearch())
}
