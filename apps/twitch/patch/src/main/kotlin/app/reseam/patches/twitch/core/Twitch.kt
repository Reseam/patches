// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.twitch.core

import app.reseam.patch.CompatiblePackage
import app.reseam.patch.Type
import app.reseam.patch.invoke
import app.reseam.patch.method

const val TWITCH_PACKAGE = "tv.twitch.android.app"

val TWITCH: CompatiblePackage = TWITCH_PACKAGE("31.9.0")

// Twitch hides its internal tools from everyone but staff.
val isStaff = method("isStaff") {
    strings("user_type", "staff")
    returns(Type.Boolean)
    paramCount(0)
}
