// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.interaction

import app.reseam.patch.ExtClass
import app.reseam.patch.Type
import app.reseam.patch.classTarget
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.Opcode
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettings
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings

private const val POINT = "android.graphics.Point"

val scrubbingPreview = patch {
    compatibleWith(YOUTUBE)
    settings(youTubeSettings, section(YouTubeSettingsPages.Controls, "Seekbar", YouTubeSettings.scrubbingPreview))

    execute {
        ScrubbingPreview.scrubberPosition.implement {
            param(0).cast(seekbarClass.descriptor).call(seekbarScrubberPosition, param(1))
            returnVoid()
        }

        gate(YouTubeSettings.scrubbingPreview) {
            // The watch player's full-size preview keeps receiving scrub events and storyboard frames
            // while it is unavailable; only its own overlay stays hidden.
            fullPreviewAvailable.alwaysReturn(false)
            fullPreviewScrub.before { call(ScrubbingPreview.onScrub, param(0), param(1)) }
            fullPreviewFrame.before { call(ScrubbingPreview.onFrame, param(0).fieldOfType("android.graphics.Bitmap")) }
            seekbarOnDraw.before { call(ScrubbingPreview.setSeekbar, thisObject) }
            filmStripStateChange.point { opcode(Opcode.IPUT_OBJECT) }
                .before { call(ScrubbingPreview.setFilmStripState, param(0)) }
        }
    }
}

private val fullPreviewTag = method("fullPreviewTag") {
    strings("player_overlay_big_boards")
    returns(Type.String)
    params()
}

private val fullPreview = classTarget("fullPreview") {
    bytecode.findClass(fullPreviewTag.owner) ?: error("The full-size scrubbing preview class is missing")
}

private val fullPreviewAvailable = method("fullPreviewAvailable") {
    inClass(fullPreview)
    flags(AccessFlags.PRIVATE or AccessFlags.FINAL)
    returns(Type.Boolean)
    params()
}

private val fullPreviewScrub = method("fullPreviewScrub") {
    inClass(fullPreview)
    params(Type.Int, Type.Long)
    returns(Type.Void)
}

private val fullPreviewFrame = method("fullPreviewFrame") {
    inClass(fullPreview)
    paramCount(1)
    returns(Type.Void)
    calls { owner("android.widget.ImageView"); name("isAttachedToWindow") }
}

private val seekbarScrubberPosition = method("seekbarScrubberPosition") {
    inClass(seekbarClass)
    flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
    params(POINT)
    returns(Type.Void)
}

private val filmStripState = klass("filmStripState") { strings("USER_MANUALLY_OPENING", "AUTO_CLOSING") }

private val filmStripStateChange = method("filmStripStateChange") {
    params(filmStripState.descriptor)
    returns(Type.Void)
}

private object ScrubbingPreview : ExtClass("app.reseam.youtube.playerui.ScrubbingPreview") {
    val onScrub by static(Type.Int, Type.Long)
    val onFrame by static("android.graphics.Bitmap")
    val setSeekbar by static(Type.View)
    val setFilmStripState by static("java.lang.Enum")
    val scrubberPosition by static(Type.View, POINT)
}
