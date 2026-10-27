// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.interaction

import app.reseam.patch.*
import app.reseam.patch.Type
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.buildInstructions
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.*
import app.reseam.patches.youtube.internal.engagementPanelHook
import app.reseam.patches.youtube.internal.playerControls
import app.reseam.patches.youtube.internal.playerTypeHook
import app.reseam.patches.youtubecommon.appHelper
import app.reseam.youtube.settings.PlayerControlsSettings
import app.reseam.youtube.settings.YouTubeStrings
import app.reseam.youtube.swipe.bindings.SwipeControls

val swipeControls = patch("Swipe controls") {
    description("Adds fullscreen volume and brightness gestures, configurable feedback, and brightness restoration.")
    compatibleWith(YOUTUBE)
    dependsOn(playerTypeHook, playerControls, engagementPanelHook)
    settings(youTubeSettings, section(YouTubeSettingsPages.Controls, YouTubeStrings.sectionSwipeControls,
        PlayerControlsSettings.swipeBrightness, PlayerControlsSettings.swipeVolume,
        PlayerControlsSettings.swipePressToEngage, PlayerControlsSettings.swipeHapticFeedback,
        PlayerControlsSettings.swipeSaveAndRestoreBrightness, PlayerControlsSettings.swipeLowestValueEnableAutoBrightness,
        PlayerControlsSettings.swipeOverlayStyle, PlayerControlsSettings.swipeOverlayBackgroundOpacity,
        PlayerControlsSettings.swipeOverlayProgressBrightnessColor, PlayerControlsSettings.swipeOverlayProgressVolumeColor,
        PlayerControlsSettings.swipeTextOverlaySize, PlayerControlsSettings.swipeOverlayTimeout,
        PlayerControlsSettings.swipeThreshold, PlayerControlsSettings.swipeVolumeSensitivity))

    execute {
        mainActivityOnCreate.after { SwipeControls.initialize(thisObject) }
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
                        moveResult(0, Type.Boolean)
                        returnValue(0)
                    })
                }
            }
            target.before { whenTrue(call(hook, thisObject, param(0))) { returnTrue() } }
        }
    }
}
