// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.reddit.navigation

import app.reseam.patch.dex.Opcode
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.reddit.core.REDDIT
import app.reseam.patches.reddit.core.RedditSettings
import app.reseam.patches.reddit.core.redditSettings

val hideNavigationTabs = patch("Hide navigation tabs") {
    description("Removes the Create or Inbox tab from the bottom bar.")
    compatibleWith(REDDIT)
    settings(redditSettings, section("Navigation", RedditSettings.hideCreateTab, RedditSettings.hideInboxTab))

    execute {
        fun label(name: String) = resources.id("string", name)?.toLong() ?: error("string/$name missing")
        val post = label("label_post")
        val inbox = label("label_inbox")
        // Builds every tab once; the tab list getter skips null tabs.
        val tabSet = method("bottomNavTabSet") { literals(label("label_home"), post) }
        fun tab(label: Long) = tabSet.point { literal(label) }.next { opcode(Opcode.IPUT_OBJECT) }.field()
        val postTab = tab(post)
        val inboxTab = tab(inbox)
        gate(RedditSettings.hideCreateTab) {
            tabSet.after { thisObject.set(postTab, nullObject) }
        }
        gate(RedditSettings.hideInboxTab) {
            tabSet.after { thisObject.set(inboxTab, nullObject) }
        }
    }
}
