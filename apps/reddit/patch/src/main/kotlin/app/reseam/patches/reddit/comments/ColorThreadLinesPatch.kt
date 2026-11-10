// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.reddit.comments

import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.before
import app.reseam.patch.dex.Opcode
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.reddit.RedditPatchStrings
import app.reseam.patches.reddit.core.REDDIT
import app.reseam.patches.reddit.core.redditSettings
import app.reseam.reddit.comments.bindings.ThreadLines
import app.reseam.reddit.settings.RedditSettings
import app.reseam.reddit.settings.RedditStrings

private const val FADED_ALPHA = 0x3ecccccdL // 0.4f
private const val MAX_FADE_DEPTH = 11L
private const val DRAW_LINE_PARAMS = 9

val colorThreadLines = patch(RedditPatchStrings.colorThreadLines) {
    description(RedditPatchStrings.colorThreadLinesDescription)
    compatibleWith(REDDIT)
    settings(redditSettings, section(RedditStrings.sectionComments, RedditSettings.colorThreadLines, RedditSettings.threadLineColors))

    execute {
        gate(RedditSettings.colorThreadLines) {
            // The loop counter, set right after the depth bounds check, is the depth of the line being drawn.
            val depth = threadLines.point { opcode(Opcode.IF_GT) }
                .next { opcode(Opcode.MOVE) }
                .captureAs("depth", Type.Int)
            // Both alpha branches meet at the next move, while most registers are still free.
            depth.next { opcode(Opcode.MOVE) }
                .captureAs("alpha", Type.Float)
                .after { capture("alpha").assign(ThreadLines.alpha(capture("alpha"), thisObject.fieldOfType(Type.Boolean))) }
            // DrawScope.drawLine(color, start, end, strokeWidth, cap, pathEffect, alpha) with its defaults mask.
            depth.next { invokeStatic { returns(Type.Void); paramCount(DRAW_LINE_PARAMS) } }
                .captureArgumentAs("color", 1, Type.Long)
                .before { capture("color").assign(ThreadLines.color(capture("depth"))) }
        }
    }
}

// Draws one vertical line per depth, fainter as it goes deeper and at 0.4 alpha in a collapsed thread.
private val threadLines = method("threadLines") {
    name("invoke")
    params(Type.Object)
    literals(FADED_ALPHA, MAX_FADE_DEPTH)
    calls { name("getLayoutDirection") }
}
