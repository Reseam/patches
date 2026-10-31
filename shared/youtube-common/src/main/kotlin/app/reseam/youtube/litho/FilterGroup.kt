// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.litho

import app.reseam.runtime.ToggleValue

abstract class FilterGroup<T>
protected constructor(
    private val setting: ToggleValue?,
    private val enabled: () -> Boolean,
    val filters: Array<out T>,
) {
    init {
        require(filters.isNotEmpty()) { "Must use one or more filter patterns (zero specified)" }
    }

    protected constructor(
        setting: ToggleValue?,
        vararg filters: T,
    ) : this(setting, { setting?.value ?: true }, filters)

    val isEnabled: Boolean
        get() = enabled()

    override fun toString(): String =
        "${javaClass.simpleName}: ${setting?.key?.substringAfter('.') ?: "(always on)"}"

    /** Returns the first matching pattern's position and length; null input never matches. */
    abstract fun check(stack: T?): FilterGroupResult

    class FilterGroupResult internal constructor(matchedIndex: Int = -1, matchedLength: Int = 0) {
        var matchedIndex: Int = matchedIndex
            private set

        var matchedLength: Int = matchedLength
            private set

        val isFiltered: Boolean
            get() = matchedIndex >= 0

        fun setValues(matchedIndex: Int, matchedLength: Int) {
            this.matchedIndex = matchedIndex
            this.matchedLength = matchedLength
        }
    }

    open class StringFilterGroup : FilterGroup<String> {
        constructor(setting: ToggleValue?, vararg filters: String) : super(setting, *filters)

        constructor(
            setting: ToggleValue?,
            enabled: () -> Boolean,
            vararg filters: String,
        ) : super(setting, enabled, filters)

        override fun check(stack: String?): FilterGroupResult {
            if (stack == null || !isEnabled || stack.isEmpty()) return FilterGroupResult()
            return filters.firstNotNullOfOrNull { pattern ->
                val index = stack.indexOf(pattern)
                if (index >= 0) FilterGroupResult(index, pattern.length) else null
            } ?: FilterGroupResult()
        }
    }

    open class ByteArrayFilterGroup : FilterGroup<ByteArray> {
        private val searchPatterns by lazy {
            filters.mapIndexed { index, pattern ->
                IndexedBytePattern(index, createFailurePattern(pattern))
            }
        }

        constructor(setting: ToggleValue?, vararg filters: ByteArray) : super(setting, *filters)

        constructor(
            setting: ToggleValue?,
            vararg filters: String,
        ) : super(setting, *convertStringsToBytes(*filters))

        constructor(
            setting: ToggleValue?,
            enabled: () -> Boolean,
            vararg filters: String,
        ) : super(setting, enabled, convertStringsToBytes(*filters))

        override fun check(stack: ByteArray?): FilterGroupResult {
            if (stack == null || !isEnabled) return FilterGroupResult()
            return searchPatterns.firstNotNullOfOrNull { pattern ->
                // Keep reading the public filters array; only the failure tables are cached.
                val filter = filters[pattern.index]
                val index = indexOf(stack, filter, pattern.failure)
                if (index >= 0) FilterGroupResult(index, filter.size) else null
            } ?: FilterGroupResult()
        }
    }
}

private data class IndexedBytePattern(val index: Int, val failure: IntArray)

// Modified implementation from https://stackoverflow.com/a/1507813
private fun indexOf(data: ByteArray, pattern: ByteArray, failure: IntArray): Int {
    if (pattern.isEmpty()) return -1
    var matched = 0
    for (index in data.indices) {
        while (matched > 0 && pattern[matched] != data[index]) matched = failure[matched - 1]
        if (pattern[matched] == data[index]) matched++
        if (matched == pattern.size) return index - pattern.size + 1
    }
    return -1
}

private fun createFailurePattern(pattern: ByteArray): IntArray {
    val failure = IntArray(pattern.size)
    var matched = 0
    for (index in 1 until pattern.size) {
        while (matched > 0 && pattern[matched] != pattern[index]) matched = failure[matched - 1]
        if (pattern[matched] == pattern[index]) matched++
        failure[index] = matched
    }
    return failure
}
