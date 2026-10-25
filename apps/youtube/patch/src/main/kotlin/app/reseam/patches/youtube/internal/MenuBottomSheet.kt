// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.internal

import app.reseam.patch.PointMatch
import app.reseam.patch.Type
import app.reseam.patch.classTarget
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.Opcode
import app.reseam.patch.method
import app.reseam.patch.methods
import app.reseam.patch.point
import app.reseam.patch.points

private const val OPTIONAL = "j\$.util.Optional"

// The menu bottom sheet's factory names the fragment key and returns the sheet.
private val menuSheetFactory = method("menu bottom sheet factory") {
    strings("MENU_BOTTOM_SHEET_FRAGMENT_KEY")
    flags(AccessFlags.STATIC)
    custom { returnType == owner }
}

/** YouTube's menu bottom sheet: the video menus of feeds, the Shorts player and older releases' Downloads. */
internal val menuSheet = classTarget("menu bottom sheet") {
    bytecode.findClass(menuSheetFactory.owner) ?: error("The menu bottom sheet class is missing")
}

// The factory's second parameter is the menu renderer the sheet shows.
internal val menuSheetRenderer = classTarget("menu renderer") {
    bytecode.findClass(menuSheetFactory.parameterTypes[1]) ?: error("The menu renderer class is missing")
}

/** Builds the sheet's rows from its menu renderer's items; the sheet's other getters do not read it. */
internal val menuSheetRows = menuSheet.methods("menu bottom sheet getters") {
    params()
    returns(OPTIONAL)
}.single { points("menu renderer reads") { menuRendererRead() }.all.isNotEmpty() }

internal val menuSheetMenu = menuSheetRows.point("menu renderer read") { menuRendererRead() }.field("menu renderer")

private fun PointMatch.menuRendererRead() {
    opcode(Opcode.IGET_OBJECT)
    field { owner(menuSheet.descriptor); type(menuSheetRenderer.descriptor) }
}

/** Handles a tapped row: routes its item's command, if any, then closes the sheet. */
internal val menuSheetTap = method("menu bottom sheet tap") {
    inClass(menuSheet)
    calls(routeEndpoint)
    paramCount(1)
    returns(Type.Void)
}
