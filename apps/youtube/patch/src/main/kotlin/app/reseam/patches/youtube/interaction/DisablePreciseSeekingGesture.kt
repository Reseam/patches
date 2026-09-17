// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.interaction

import app.reseam.patch.Type
import app.reseam.patch.classTarget
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.Opcode
import app.reseam.patch.method
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.settings.before
import app.reseam.patch.settings.returnFalseWhen
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettings
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings

val disablePreciseSeekingGesture = patch {
    compatibleWith(YOUTUBE)
    settings(youTubeSettings, section(YouTubeSettingsPages.Controls, "Seekbar", YouTubeSettings.disablePreciseSeekingGesture))

    execute {
        allowSwipingUpGesture.before(YouTubeSettings.disablePreciseSeekingGesture) { returnVoid() }
        showSwipingUpGuide.returnFalseWhen(YouTubeSettings.disablePreciseSeekingGesture)
    }
}

private const val SWIPE_UP_FEATURE_FLAG = 45379021L

private val swipingUpGestureParent = method("swipingUpGestureParent") {
    literals(SWIPE_UP_FEATURE_FLAG)
    returns(Type.Boolean)
    params()
}

internal val swipeClass = classTarget("swipeClass") {
    bytecode.findClass(swipingUpGestureParent.owner) ?: error("swipe gesture class is missing")
}

private val allowSwipingUpGestureCandidates = methods("allowSwipingUpGesture") {
    inClass(swipeClass)
    flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
    returns(Type.Void)
    paramCount(1)
}

private val allowSwipingUpGesture = allowSwipingUpGestureCandidates.single {
    parameterTypes.singleOrNull()?.startsWith("L") == true
}

private val showSwipingUpGuide = method("showSwipingUpGuide") {
    inClass(swipeClass)
    flags(AccessFlags.FINAL)
    returns(Type.Boolean)
    params()
    // The guide checks gesture-enabled booleans; the active-gesture predicate reads an enum.
    opcode(Opcode.IGET_BOOLEAN)
}
