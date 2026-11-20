// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.twitch.core

import app.reseam.patch.MethodTarget
import app.reseam.patch.PatchRuntime
import app.reseam.patch.PointTarget
import app.reseam.patch.Type
import app.reseam.patch.method
import app.reseam.patch.point

// The native player overlay's constructor looks up each of its views by id.
fun PatchRuntime.playerOverlay(): MethodTarget =
    method("playerOverlay") { literals(resourceId("id", "settings_button"), resourceId("id", "play_pause_fast_seek_view")) }

/** The view [method] looks up by the id [name], captured as `view`. */
fun PatchRuntime.viewLookup(method: MethodTarget, name: String): PointTarget =
    method.point("${name}Lookup") { literal(resourceId("id", name)) }.next { resultOf(Type.View) }.captureAs("view")
