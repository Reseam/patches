// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.x.core

import app.reseam.patch.PatchRuntime
import app.reseam.patch.PointTarget
import app.reseam.patch.Type
import app.reseam.patch.method
import app.reseam.patch.point

/** The drawer is one composable that makes a fixed row call per entry, each right after loading the row's [label]. */
fun PatchRuntime.drawerRow(label: String): PointTarget {
    fun id(name: String) = resources.id("string", name)?.toLong() ?: error("string/$name missing")
    val labelId = id(label)
    return method("drawerMenu") { literals(id("drawer_settings_title")) }
        .point { literal(labelId) }
        .next { invokeStatic { returns(Type.Void); hasParam(Type.String) } }
}
