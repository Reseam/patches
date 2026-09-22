// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.interaction

import app.reseam.patch.*
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.buildInstructions
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.*
import app.reseam.patches.youtube.internal.appHelper
import app.reseam.patches.youtube.internal.playerControls
import app.reseam.patches.youtube.internal.playerTypeHook

val swipeControls = patch("Swipe controls") {
    description("Adds fullscreen volume and brightness gestures, configurable feedback, and brightness restoration.")
    compatibleWith(YOUTUBE)
    dependsOn(playerTypeHook, playerControls)
    settings(youTubeSettings, section(YouTubeSettingsPages.Controls, "Swipe controls",
        YouTubeSettings.swipeBrightness, YouTubeSettings.swipeVolume,
        YouTubeSettings.swipePressToEngage, YouTubeSettings.swipeHapticFeedback,
        YouTubeSettings.swipeSaveAndRestoreBrightness, YouTubeSettings.swipeLowestValueEnableAutoBrightness,
        YouTubeSettings.swipeOverlayStyle, YouTubeSettings.swipeOverlayBackgroundOpacity,
        YouTubeSettings.swipeOverlayProgressBrightnessColor, YouTubeSettings.swipeOverlayProgressVolumeColor,
        YouTubeSettings.swipeTextOverlaySize, YouTubeSettings.swipeOverlayTimeout,
        YouTubeSettings.swipeThreshold, YouTubeSettings.swipeVolumeSensitivity))

    execute {
        mainActivityOnCreate.after { call(SwipeControls.initialize, thisObject) }
        val activity = klass(MAIN_ACTIVITY)
        listOf(
            Triple("dispatchTouchEvent", "android.view.MotionEvent", SwipeControls.touch),
            Triple("dispatchKeyEvent", "android.view.KeyEvent", SwipeControls.key),
        ).forEach { (name, event, hook) ->
            val proto = "(L${event.replace('.', '/')};)Z"
            val existing = activity.classDef.method(name, proto)
            val target = if (existing != null) {
                methodTarget("activity $name") { existing }
            } else {
                // Add only the required override. No inserted superclass, constructor rewriting,
                // or blanket removal of final flags across the activity hierarchy.
                var ancestor = activity.classDef.info.superclass?.let(bytecode::findClass)
                while (ancestor != null) {
                    val inherited = ancestor.method(name, proto)
                    if (inherited != null) {
                        inherited.setAccessFlags(inherited.info.accessFlags.toInt() and AccessFlags.FINAL.inv())
                        break
                    }
                    ancestor = ancestor.info.superclass?.let(bytecode::findClass)
                }
                appHelper(activity.classDef, name, proto).also {
                    it.method.replaceBody(3, 2, buildInstructions {
                        invokeSuper(activity.classDef.info.superclass!!, name, proto, 1, 2)
                        moveResult(0)
                        returnValue(0)
                    })
                }
            }
            target.before { whenTrue(call(hook, thisObject, param(0))) { returnTrue() } }
        }
    }
}

private object SwipeControls : ExtClass("app.reseam.youtube.swipe.SwipeControls") {
    val initialize = static("initialize", Type.Activity)
    val touch = static("touch", Type.Activity, "android.view.MotionEvent", returns = Type.Boolean)
    val key = static("key", Type.Activity, "android.view.KeyEvent", returns = Type.Boolean)
}
