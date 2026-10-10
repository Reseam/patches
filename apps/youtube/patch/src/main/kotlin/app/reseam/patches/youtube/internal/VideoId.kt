// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.internal

import app.reseam.patch.ExtMethod
import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtubecommon.videoIdGetter
import app.reseam.patches.youtubecommon.videoIdMethod

private val videoIdHooks = mutableListOf<ExtMethod>()
private val backgroundPlayVideoIdHooks = mutableListOf<ExtMethod>()

/** Runs after YouTube obtains the ID of a newly opened regular or Shorts video. */
fun hookVideoId(hook: ExtMethod) {
    require(hook.isStatic && hook.proto == "(Ljava/lang/String;)V") {
        "A video-id hook must be static (String) -> Unit: $hook"
    }
    videoIdHooks += hook
}

/** Runs after YouTube obtains an ID while preparing background playback. */
fun hookBackgroundPlayVideoId(hook: ExtMethod) {
    require(hook.isStatic && hook.proto == "(Ljava/lang/String;)V") {
        "A background video-id hook must be static (String) -> Unit: $hook"
    }
    backgroundPlayVideoIdHooks += hook
}

/** Runs after the player-response parser obtains its video ID. */
fun hookPlayerResponseVideoId(hook: ExtMethod) {
    registerPlayerResponseVideoIdHook(hook)
}

/** Hooks the three video-ID paths used by later video patches. */
val videoIdHook = patch {
    compatibleWith(YOUTUBE)
    dependsOn(playerResponseHook)

    afterDependents {
        videoIdMethod.point {
            calls(videoIdGetter)
        }.next { resultOf(Type.String) }
            .captureAs("videoId", Type.String)
            .after {
                videoIdHooks.forEach { call(it, capture("videoId")) }
            }

        backgroundPlayVideoIdMethod.point {
            calls(videoIdGetter)
        }.next { resultOf(Type.String) }
            .captureAs("backgroundVideoId", Type.String)
            .after {
                backgroundPlayVideoIdHooks.forEach { call(it, capture("backgroundVideoId")) }
            }

        videoIdHooks.clear()
        backgroundPlayVideoIdHooks.clear()
    }
}

// The background path calls the same video-ID getter as the foreground callback.
val backgroundPlayVideoIdMethod = method("backgroundPlayVideoIdMethod") {
    calls(videoIdGetter)
    flags(AccessFlags.DECLARED_SYNCHRONIZED)
    returns(Type.Void)
    paramCount(1)
}
