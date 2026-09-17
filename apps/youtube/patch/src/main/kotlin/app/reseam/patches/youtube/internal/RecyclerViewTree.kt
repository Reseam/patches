// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.internal

import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.Opcode
import app.reseam.patch.method
import app.reseam.patch.point

private const val RECYCLER_VIEW = "android.support.v7.widget.RecyclerView"

/**
 * Every litho list the app draws is bound by one class, which is handed the `RecyclerView` it binds
 * to. The name the binder gives its component tree is what finds it.
 */
val lithoRecyclerViewBinder = method("lithoRecyclerViewBinder") {
    strings("LithoRVSLCBinder")
    flags(AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR)
    hasParam(RECYCLER_VIEW)
}

/**
 * Where the binder attaches its own listeners to the list. Patches that watch a list's views, such
 * as the video quality and playback speed flyouts, emit here with `before { }` and take the list
 * from `param(1)`.
 */
val lithoRecyclerViewAttached = lithoRecyclerViewBinder.point("lithoRecyclerViewAttached") {
    opcode(Opcode.CHECK_CAST)
    then { opcode(Opcode.NEW_INSTANCE) }
    then { opcode(Opcode.INVOKE_DIRECT) }
    then { invokeVirtual { owner(RECYCLER_VIEW); paramCount(1) } }
}
