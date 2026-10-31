// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.internal

import app.reseam.patch.ExtMethod
import app.reseam.patch.RunList
import app.reseam.patch.Type
import app.reseam.patch.before
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patches.youtube.core.YOUTUBE

private val beforeVideoIdHooks = RunList<ExtMethod>()

/** Runs before the player-response video-id hook. The hook is `(String, String, Boolean) -> String`. */
fun hookBeforeVideoId(hook: ExtMethod) {
    require(hook.isStatic && hook.proto == "(Ljava/lang/String;Ljava/lang/String;Z)Ljava/lang/String;") {
        "A player-response before-video-id hook must be static (String, String, Boolean) -> String: $hook"
    }
    beforeVideoIdHooks.add(hook)
}

/** Runs when the player-response parser receives its video id. The hook is `(String, Boolean) -> Unit`. */
fun registerPlayerResponseVideoIdHook(hook: ExtMethod) {
    require(hook.isStatic && hook.proto == "(Ljava/lang/String;Z)V") {
        "A player-response video-id hook must be static (String, Boolean) -> Unit: $hook"
    }
    playerResponseVideoIdHooks.add(hook)
}

private val playerResponseVideoIdHooks = RunList<ExtMethod>()

/** The player-request builder and its patch-time hook registration point. */
val playerResponseHook = patch {
    compatibleWith(YOUTUBE)

    afterDependents {
        if (beforeVideoIdHooks.values.isEmpty() && playerResponseVideoIdHooks.values.isEmpty()) return@afterDependents

        playerParameterBuilder.before {
            // Video ID, player parameters and the opening/playing flag.
            beforeVideoIdHooks.values.forEach { hook ->
                param(2).assign(call(hook, param(2), param(0), param(12)))
            }
            playerResponseVideoIdHooks.values.forEach { hook ->
                call(hook, param(0), param(12))
            }
        }
    }
}

private val playerRequest = klass("playerRequest") { strings("dataExpiredForSeconds") }

val playerParameterBuilder = method("playerParameterBuilder") {
    returns(playerRequest.descriptor)
    hasParam("j$.time.Duration")
    // Only the parameters consumed by hooks form the contract.
    param(0, Type.String)
    param(2, Type.String)
    param(12, Type.Boolean)
}
