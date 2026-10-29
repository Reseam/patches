// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("PinFeeds")

package app.reseam.pinterest.feed

import app.reseam.pinterest.refs.isPin
import app.reseam.pinterest.refs.promoted
import app.reseam.pinterest.refs.shoppable
import app.reseam.runtime.Export

@Export fun isProductGroup(item: Any?): Boolean = false

@Export
fun removePromoted(models: MutableList<Any?>, ids: MutableList<Any?>) =
    removeIf(models, ids) { isPin(it) && promoted(it!!) == true }

@Export
fun removeShopping(models: MutableList<Any?>, ids: MutableList<Any?>) =
    removeIf(models, ids) { isProductGroup(it) || isPin(it) && shoppable(it!!) == true }

// The feed keeps each model's id at the same position in a second list.
private fun removeIf(models: MutableList<Any?>, ids: MutableList<Any?>, hidden: (Any?) -> Boolean) {
    val kept = models.zip(ids).filterNot { (model, _) -> hidden(model) }
    models.replaceWith(kept.map { it.first })
    ids.replaceWith(kept.map { it.second })
}

private fun <T> MutableList<T>.replaceWith(items: List<T>) {
    clear()
    addAll(items)
}
