// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.interaction

import app.reseam.patch.Type
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.points
import app.reseam.patch.settings.after
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettings
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings

val enableSlideToSeek = patch {
    compatibleWith(YOUTUBE)
    settings(youTubeSettings, section(YouTubeSettingsPages.Controls, "Seekbar", YouTubeSettings.slideToSeek))

    execute {
        val abCheck = slideToSeekMethod
            .point("slideToSeekCheck") { invokeVirtual { returns(Type.Boolean); paramCount(0) } }
            .callee()
        methods("slideToSeekCallers") {
            calls(abCheck)
        }.points { calls(abCheck) }.forEach {
            this
                .next { resultOf(Type.Boolean) }
                .captureAs("disabled", Type.Boolean)
                .after(YouTubeSettings.slideToSeek) { capture("disabled").assign(bool(false)) }
        }

        fastForwardGesture
            .point { invokeVirtual { returns(Type.Boolean); params() } }
            .next { resultOf(Type.Boolean) }
            .captureAs("fastForward", Type.Boolean)
            .after(YouTubeSettings.slideToSeek) { capture("fastForward").assign(bool(false)) }
    }
}

private val slideToSeekMethod = method("slideToSeekMethod") {
    flags(AccessFlags.PRIVATE or AccessFlags.FINAL)
    returns(Type.Void)
    params(Type.View, Type.Float)
    literals(67108864L)
}

private val fastForwardGesture = method("fastForwardGesture") {
    flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
    returns(Type.Boolean)
    params()
    calls { owner(swipeClass.descriptor); returns(Type.Boolean) }
    inClass(klass("com.google.android.apps.youtube.app.watch.nextgenwatch.ui.NextGenWatchLayout"))
}
