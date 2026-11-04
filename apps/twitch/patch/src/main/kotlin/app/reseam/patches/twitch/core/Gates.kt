// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.twitch.core

import app.reseam.patch.PointTarget
import app.reseam.patch.PointsTarget
import app.reseam.patch.Type
import app.reseam.patch.settings.GateScope

// Nothing listens on the discard port, so a request sent there fails at once.
private const val DISCARD_SERVER = "https://127.0.0.1:9/"

/** Makes the boolean returned by the call at [call] read as true. */
fun GateScope.forceTrue(call: PointTarget) {
    call.next { resultOf(Type.Boolean) }.captureAs("result").after { capture("result").assign(bool(true)) }
}

/** Points every URL on [server] at the discard port, and returns how many it changed. */
fun GateScope.cutOffServer(urls: PointsTarget, server: String): Int {
    val points = urls.all
    require(points.isNotEmpty()) { "The app no longer references $server" }
    points.forEach { url ->
        url.captureAs("url").after {
            capture("url").assign(
                capture("url").callVirtual(
                    "java.lang.String", "replace", "(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;",
                    string(server), string(DISCARD_SERVER),
                ),
            )
        }
    }
    return points.size
}
