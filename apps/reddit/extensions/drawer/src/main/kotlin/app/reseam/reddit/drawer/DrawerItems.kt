// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("DrawerItems")

package app.reseam.reddit.drawer

import app.reseam.reddit.settings.RedditSettings
import app.reseam.runtime.Export

// R8 renames the row models, so the body is emitted by the drawer patch. Null is any other row.
@Export external fun row(item: Any?): DrawerRow?

/**
 * The drawer is one flat list: a divider, a section header, then that section's rows. A hidden
 * section goes with its divider.
 */
@Export
fun without(items: List<Any?>): List<Any?> {
    val hidden = hiddenRows()
    if (hidden.isEmpty()) return items
    val kept = ArrayList<Any?>(items.size)
    var inHiddenSection = false
    for (item in items) {
        val row = row(item)
        if (row == DrawerRow.DIVIDER || row in sectionHeaders) inHiddenSection = row in hidden
        if (inHiddenSection || row in hidden) continue
        if (row == DrawerRow.DIVIDER && row(kept.lastOrNull()) == DrawerRow.DIVIDER) continue
        kept += item
    }
    if (kept.isNotEmpty() && row(kept.first()) == DrawerRow.DIVIDER) kept.removeAt(0)
    return kept
}

private val sectionHeaders = setOf(DrawerRow.HEADER, DrawerRow.REDDIT_PRO, DrawerRow.RESOURCES)

private fun hiddenRows() = buildSet {
    if (RedditSettings.hideDrawerPopular.value) add(DrawerRow.POPULAR)
    if (RedditSettings.hideDrawerNews.value) add(DrawerRow.NEWS)
    if (RedditSettings.hideDrawerLatest.value) add(DrawerRow.LATEST)
    if (RedditSettings.hideDrawerRedditPro.value) add(DrawerRow.REDDIT_PRO)
    if (RedditSettings.hideDrawerResources.value) add(DrawerRow.RESOURCES)
}
