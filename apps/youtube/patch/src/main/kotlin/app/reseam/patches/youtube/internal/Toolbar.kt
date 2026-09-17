// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.internal

import app.reseam.patch.Type
import app.reseam.patch.classTarget
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.Opcode
import app.reseam.patch.fieldOfType
import app.reseam.patch.method
import app.reseam.patch.point

private const val RESOURCES = "android.content.res.Resources"

/**
 * Every button in the top toolbar, cast, notifications, search and the account avatar, is built by
 * this one method from the menu item the toolbar is inflating.
 */
val toolbarButtonCreate = method("toolbarButtonCreate") {
    flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
    returns(Type.Void)
    hasParam("android.view.MenuItem")
    calls { owner("Landroid/widget/ImageView;"); name("setImageDrawable") }
    calls { owner("Landroid/content/res/Resources;"); name("getDrawable") }
}

val toolbarButtonClass = classTarget("toolbarButtonClass") {
    bytecode.findClass(toolbarButtonCreate.owner) ?: error("The toolbar button class is missing")
}

/** The button holds the one image view it draws its icon into. */
val toolbarButtonIcon = toolbarButtonClass.fieldOfType("android.widget.ImageView")

/**
 * Which button this is, as the enum the icon is looked up from. The enum has a null default the
 * app substitutes right after producing it, so the capture is taken at the value and the point is
 * stepped past the branch the substitution ends on: patches emit with `after { }`, and read the
 * button as `capture("toolbarButton")` and its view as `thisObject.field(toolbarButtonIcon)`.
 */
val toolbarButtonIdentified = toolbarButtonCreate
    .point("toolbarButtonIdentified") { invokeVirtual { owner(RESOURCES); name("getDrawable") } }
    .previous { opcode(Opcode.MOVE_RESULT_OBJECT) }
    .captureAs("toolbarButton")
    .next { opcode(Opcode.IGET_OBJECT) }
