// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.internal

import app.reseam.patch.Type
import app.reseam.patch.classTarget
import app.reseam.patch.method

/** Resolves a menu command, the ⋮ of a card or player, into the menu it shows; param 0 is the command. */
internal val menuCommandResolver = method("menu command resolver") {
    strings("com.google.android.libraries.youtube.rendering.elements.sender_view", "com.google.android.libraries.youtube.innertube.endpoint.tag")
    calls { owner("android.view.Window"); name("getNavigationBarColor") }
    hasParam("java.util.Map")
    returns(Type.Void)
}

internal val commandClass = classTarget("command") {
    bytecode.findClass(menuCommandResolver.parameterTypes.first()) ?: error("The command class is missing")
}
