// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.x.timeline

import app.reseam.patch.Type
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.x.core.MODIFIER
import app.reseam.patches.x.core.X
import app.reseam.patches.x.core.XSettingsPages
import app.reseam.x.settings.XSettings
import app.reseam.patches.x.core.xSettings
import app.reseam.x.settings.XStrings

val hidePostMetrics = patch("Hide post metrics") {
    description("Hides the view, reply, repost, like and bookmark counts on posts. The buttons stay and keep working.")
    compatibleWith(X)
    settings(
        xSettings,
        section(
            XSettingsPages.Home,
            XStrings.sectionTimeline,
            XSettings.hideViewCounts,
            XSettings.hideReplyCounts,
            XSettings.hideRepostCounts,
            XSettings.hideLikeCounts,
            XSettings.hideBookmarkCounts,
        ),
    )

    execute {
        // The action type enum keeps its constant names for serialization.
        val actionType = inlineActionButton.parameterTypes[0]
        gate(XSettings.hideViewCounts) {
            inlineActionButton.before {
                whenEqual(param(0), enumValue(actionType, "ViewCount")) { returnVoid() }
            }
        }

        // The button's first String is the count its caller formatted. X draws no number for a null,
        // blank or "0" count, so the button keeps the layout it has at zero. The raw count stays: it
        // picks the like animation.
        val countText = inlineActionButton.parameterTypes.indexOf(Type.String)
        val counts = mapOf(
            XSettings.hideReplyCounts to listOf("Reply"),
            XSettings.hideRepostCounts to listOf("Retweet", "UndoRetweet"),
            XSettings.hideLikeCounts to listOf("Favorite", "Unfavorite"),
            XSettings.hideBookmarkCounts to listOf("AddToBookmarks", "RemoveFromBookmarks"),
        )
        for ((toggle, actions) in counts) {
            gate(toggle) {
                inlineActionButton.before {
                    for (action in actions) {
                        whenEqual(param(0), enumValue(actionType, action)) { param(countText).assign(nullObject) }
                    }
                }
            }
        }

        // The detail line appends "N Views" only for a non-null count.
        val viewCount = resources.id("string", "view_count") ?: error("string/view_count missing")
        gate(XSettings.hideViewCounts) {
            methods("focalViewCount") { literals(viewCount.toLong()) }.forEach {
                before { paramOfType("java.lang.Long").assign(nullObject) }
            }
        }
    }
}

private val inlineActionType = klass("inlineActionType") { enumConstants("ViewCount", "UndoRetweet") }

// The action button is the only composable taking an action type. The action bar and the button's
// own recompose lambda both call it, so either caller leads to the same method.
private val inlineActionCaller = method("inlineActionCaller") {
    calls { hasParam(inlineActionType.descriptor); hasParam(MODIFIER); returns(Type.Void) }
    first()
}

val inlineActionButton = inlineActionCaller
    .point { invokeStatic { hasParam(inlineActionType.descriptor); hasParam(MODIFIER) } }
    .callee("inlineActionButton")
