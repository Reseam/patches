// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.internal

import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patches.youtube.core.YOUTUBE

/**
 * The feed refuses to refresh when its first component is an empty one, because the swipe layout
 * asks the View whether it can scroll and gets yes. Only the View fallback is disabled; the ListView
 * branch above it keeps its own answer.
 */
val verticalScrollFix = patch {
    compatibleWith(YOUTUBE)

    execute {
        canScrollVertically
            .point("viewCanScrollVertically") {
                invokeVirtual { name("canScrollVertically"); params(Type.Int); returns(Type.Boolean) }
            }.next { resultOf(Type.Boolean) }
            .captureAs("canScroll", Type.Boolean)
            .after { capture("canScroll").assign(bool(false)) }
    }
}

val canScrollVertically = method("canScrollVertically") {
    inClass(klass("androidx.swiperefreshlayout.widget.SwipeRefreshLayout"))
    flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
    params()
    returns(Type.Boolean)
    calls { name("canScrollVertically") }
}
