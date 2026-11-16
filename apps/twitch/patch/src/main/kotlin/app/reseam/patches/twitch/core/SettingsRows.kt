// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.twitch.core

import app.reseam.patch.PointTarget
import app.reseam.patch.Type

/** The next call that draws a row of Twitch's settings list. */
fun PointTarget.settingsRowCall(): PointTarget = next { invokeStatic { returns(Type.Void); paramCount(8); hasParam(Type.String); hasParam(Type.Int) } }
