// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.litho

import app.reseam.runtime.Export
import app.reseam.runtime.ToggleValue
import app.reseam.youtube.litho.FilterGroup.StringFilterGroup

/**
 * Register callbacks during construction, before the patch registers the filter at process start.
 */
@Export
abstract class Filter {
    enum class FilterContentType {
        IDENTIFIER,
        PATH,
    }

    val identifierCallbacks = mutableListOf<StringFilterGroup>()
    val pathCallbacks = mutableListOf<StringFilterGroup>()

    protected fun addIdentifierCallbacks(vararg groups: StringFilterGroup) {
        identifierCallbacks += groups
    }

    protected fun addPathCallbacks(vararg groups: StringFilterGroup) {
        pathCallbacks += groups
    }

    /** Runs off the main thread once an enabled [group] matched [component]. */
    open fun isFiltered(component: LithoComponent, group: StringFilterGroup): Boolean =
        (group as? FilterRule)?.accepts?.invoke(component) ?: true
}

/** A group that hides a match only when [accepts] agrees. */
open class FilterRule(
    setting: ToggleValue?,
    vararg patterns: String,
    val accepts: (LithoComponent) -> Boolean = { true },
) : StringFilterGroup(setting, *patterns)

/**
 * The matched component. [buffer] is its tree root's proto, shared by every component in the tree;
 * [elementBuffer] is the component's own element. Both are encoded on first use, without theme
 * resources, and can be empty, as can [accessibility].
 */
class LithoComponent
internal constructor(
    val identifier: String,
    val path: String,
    val accessibility: String,
    private val lookup: Lazy<ByteArray>,
    private val element: Lazy<ByteArray>,
    val type: Filter.FilterContentType,
    val index: Int,
) {
    val buffer: ByteArray
        get() = lookup.value

    val elementBuffer: ByteArray
        get() = element.value
}
