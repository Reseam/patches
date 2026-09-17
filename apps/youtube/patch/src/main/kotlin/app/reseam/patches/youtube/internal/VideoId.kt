// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.internal

import app.reseam.patch.ExtMethod
import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.parameterTypes
import app.reseam.patch.dex.returnType
import app.reseam.patch.classTarget
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patches.youtube.core.YOUTUBE

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

/** The response builder method whose owner is the regular video-ID class. */
val videoIdParentMethod = method("videoIdParentMethod") {
    flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
    paramCount(1)
    literals(524288)
    custom {
        returnType.startsWith("[L") && parameterTypes.singleOrNull()?.startsWith("L") == true
    }
}

/** The response builder class reached from the resolved parent method owner. */
private val videoIdParentClass = classTarget("videoIdParentClass") {
    bytecode.findClass(videoIdParentMethod.owner) ?: error("The video-ID parent class is missing")
}

/** The child method that extracts the ID and puts it into the response map. */
val videoIdMethod = method("videoIdMethod") {
    inClass(videoIdParentClass)
    flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
    returns(Type.Void)
    paramCount(1)
    calls { returns(Type.String); params() }
    custom { parameterTypes.singleOrNull()?.startsWith("L") == true }
}

// The background path stores the same video-ID getter used by the foreground callback.
internal val videoIdGetter = videoIdMethod.point {
    invokeInterface { params(); returns(Type.String) }
}.callee()

val backgroundPlayVideoIdMethod = method("backgroundPlayVideoIdMethod") {
    calls(videoIdGetter)
    flags(AccessFlags.DECLARED_SYNCHRONIZED)
    returns(Type.Void)
    paramCount(1)
}
