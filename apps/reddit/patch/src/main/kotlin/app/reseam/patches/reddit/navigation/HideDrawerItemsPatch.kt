// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.reddit.navigation

import app.reseam.patch.Type
import app.reseam.patch.before
import app.reseam.patch.descriptor
import app.reseam.patch.dex.Opcode
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.section
import app.reseam.patches.reddit.RedditPatchStrings
import app.reseam.patches.reddit.core.REDDIT
import app.reseam.patches.reddit.core.redditSettings
import app.reseam.reddit.drawer.bindings.DrawerItems
import app.reseam.reddit.drawer.bindings.DrawerRow
import app.reseam.reddit.settings.RedditSettings
import app.reseam.reddit.settings.RedditStrings

private const val HEADER_ITEM = "com.reddit.screens.drawer.community.HeaderItem"
private const val BIND_LIST_ON_VIEW = "com.reddit.screens.drawer.community.CommunityDrawerPresenter\$bindListOnView\$2"

val hideDrawerItems = patch(RedditPatchStrings.hideDrawerItems) {
    description(RedditPatchStrings.hideDrawerItemsDescription)
    compatibleWith(REDDIT)
    settings(
        redditSettings,
        section(
            RedditStrings.sectionDrawer,
            RedditSettings.hideDrawerPopular,
            RedditSettings.hideDrawerNews,
            RedditSettings.hideDrawerLatest,
            RedditSettings.hideDrawerRedditPro,
            RedditSettings.hideDrawerResources,
        ),
    )

    execute {
        fun label(name: String) = resources.id("string", name)?.toInt() ?: error("string/$name missing")
        // The feed links are the call-to-action rows titled with the feeds' labels.
        val links = mapOf(
            "POPULAR" to label("popular_feed_label"),
            "NEWS" to label("news_feed_label"),
            "LATEST" to label("latest_feed_label"),
        )

        DrawerItems.row.implement {
            val item = param(0)
            whenInstanceOf(item, divider.descriptor) { returnValue(enumValue(DrawerRow.descriptor, "DIVIDER")) }
            whenInstanceOf(item, header.descriptor) {
                val type = item.cast(header.descriptor).fieldOfType(descriptor(HEADER_ITEM))
                for (section in listOf("REDDIT_PRO", "RESOURCES")) {
                    whenEqual(type, enumValue(HEADER_ITEM, section)) { returnValue(enumValue(DrawerRow.descriptor, section)) }
                }
                returnValue(enumValue(DrawerRow.descriptor, "HEADER"))
            }
            whenInstanceOf(item, link.descriptor) {
                val title = item.cast(link.descriptor).field(linkTitle)
                for ((row, id) in links) {
                    whenEqual(title, int(id)) { returnValue(enumValue(DrawerRow.descriptor, row)) }
                }
            }
            returnValue(nullObject)
        }

        // The click handlers index the stored list, so the rows are dropped before it is stored.
        drawerList.point { opcode(Opcode.IPUT_OBJECT); field { type(Type.List) } }
            .captureAs("items", Type.List)
            .before { capture("items").assign(DrawerItems.without(capture("items"))) }
    }
}

// The presenter builds the whole drawer, then launches the coroutine that binds it.
private val drawerList = method("drawerList") {
    returns(Type.Void)
    params()
    calls { owner(BIND_LIST_ON_VIEW); name("<init>") }
}

private val header = klass("drawerHeader") { strings("HeaderItemUiModel(uniqueId=") }
private val divider = klass("drawerDivider") { strings("DividerItemUiModel(uniqueId=-9223372036854775808)") }
private val link = klass("drawerLink") { strings("CTAItemUiModel(uniqueId=") }
private val linkTitle = link.method("toString").point { string(", titleResId=") }.next { opcode(Opcode.IGET) }.field("drawerLinkTitle")
