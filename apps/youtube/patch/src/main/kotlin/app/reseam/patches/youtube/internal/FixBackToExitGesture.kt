// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.internal

import app.reseam.patch.ExtClass
import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.before
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.Opcode
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.points
import app.reseam.patches.youtube.core.MAIN_ACTIVITY
import app.reseam.patches.youtube.core.YOUTUBE

private const val RECYCLER_VIEW = "android.support.v7.widget.RecyclerView"
private const val ITERATOR = "java.util.Iterator"

/**
 * Hiding the first feed component leaves the feed thinking it is not at the top, which breaks the
 * back gesture that should exit the app. Tracks whether the feed is scrolled to the top so the
 * back press can finish the activity itself.
 */
val fixBackToExitGesture = patch {
    compatibleWith(YOUTUBE)

    execute {
        restoreScrollPosition
            .points("savedScrollPosition") {
                invokeVirtual {
                    owner("android.os.Bundle")
                    name("getInt")
                    params(Type.String, Type.Int)
                    returns(Type.Int)
                }
                argument(1) { string("scroll_position") }
            }.single()
            .next { resultOf(Type.Int) }.captureAs("position")
            .after { call(FixBackToExitGesture.onScrollPositionRestored, capture("position")) }

        // The join after the scroll-to-top loop is a branch target, so the hook goes after the
        // instruction there rather than before it, where the branch would skip it.
        scrollFeedsToTop.forEach {
            point("scrollFeedsToTopDone") { invokeInterface { owner(ITERATOR); name("next") } }
                .next { opcode(Opcode.GOTO) }
                .next { opcode(Opcode.IGET_OBJECT) }
                .after { call(FixBackToExitGesture.onTopView) }
        }

        mainActivityOnBackPressed
            .point("mainActivityOnBackPressedReturn") { opcode(Opcode.RETURN_VOID) }
            .before { call(FixBackToExitGesture.onBackPressed, thisObject.cast(Type.Activity)) }
    }
}

object FixBackToExitGesture : ExtClass("app.reseam.youtube.misc.FixBackToExitGesture") {
    val onTopView = static("onTopView")
    val onScrollPositionRestored = static("onScrollPositionRestored", Type.Int)
    val onBackPressed = static("onBackPressed", Type.Activity)
}

// Restores the feed position from its state bundle, whether scrolling is inline or delegated.
val restoreScrollPosition = method("restoreScrollPosition") {
    strings("scroll_position")
    params("android.os.Bundle")
    returns(Type.Void)
}

// Methods that scroll registered feed RecyclerViews to position 0, including instant
// and smooth variants. Reaching the top through any of them counts.
val scrollFeedsToTop = methods("scrollFeedsToTop") {
    flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
    params()
    returns(Type.Void)
    calls { owner(ITERATOR); name("next") }
    calls { owner(RECYCLER_VIEW); params(Type.Int); returns(Type.Void) }
    opcodeSequence(
        Opcode.INVOKE_INTERFACE,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.CHECK_CAST,
        Opcode.CONST_4,
        Opcode.INVOKE_VIRTUAL,
        Opcode.GOTO,
    )
}

val mainActivityOnBackPressed = method("mainActivityOnBackPressed") {
    inClass(klass(MAIN_ACTIVITY))
    name("onBackPressed")
    flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
    params()
    returns(Type.Void)
}
