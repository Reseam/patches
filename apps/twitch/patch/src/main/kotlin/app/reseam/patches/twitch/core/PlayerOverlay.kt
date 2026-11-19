// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.twitch.core

import app.reseam.patch.MethodTarget
import app.reseam.patch.PatchRuntime
import app.reseam.patch.PointTarget
import app.reseam.patch.Type
import app.reseam.patch.method
import app.reseam.patch.point

/** Returns the native player overlay's constructor, which looks up each of its views by id. Resource ids exist only at patch time. */
fun PatchRuntime.playerOverlay(): MethodTarget = method("playerOverlay") { literals(viewId("settings_button"), viewId("play_pause_fast_seek_view")) }

/** Returns the native player overlay's lookup of its settings gear, with the gear captured as `settingsButton`. Resource ids exist only at patch time. */
fun PatchRuntime.playerSettingsButton(): PointTarget {
    val gear = viewId("settings_button")
    return playerOverlay()
        .point("settingsButtonLookup") {
            invokeVirtual { name("findViewById") }
            argument(1) { literal(gear) }
        }
        .next { resultOf(Type.View) }
        .captureAs("settingsButton")
}

/** Returns the id of the view resource [name]. */
fun PatchRuntime.viewId(name: String): Long = resources.id("id", name)?.toLong() ?: error("id/$name missing")
