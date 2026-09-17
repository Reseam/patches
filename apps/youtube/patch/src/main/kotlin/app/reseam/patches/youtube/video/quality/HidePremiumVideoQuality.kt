// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.video.quality

import app.reseam.patch.ExtClass
import app.reseam.patch.Type
import app.reseam.patch.before
import app.reseam.patch.classTarget
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.Opcode
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettings
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.patches.youtube.internal.videoInformationHook
import app.reseam.patches.youtube.internal.videoQualityClass

private const val EXTENSION_VIDEO_QUALITY_ARRAY = "[Lapp/reseam/youtube/video/VideoInformation\$VideoQualityInterface;"

object HidePremiumVideoQuality : ExtClass("app.reseam.youtube.quality.HidePremiumVideoQuality") {
    val hidePremiumVideoQuality = static("hidePremiumVideoQuality", EXTENSION_VIDEO_QUALITY_ARRAY, returns = EXTENSION_VIDEO_QUALITY_ARRAY)
}

private val currentVideoFormatToString = method("current video format toString") {
    name("toString")
    flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
    params()
    returns(Type.String)
    strings("currentVideoFormat=")
}

private val currentVideoFormatClass = classTarget("current video format class") {
    bytecode.findClass(currentVideoFormatToString.owner) ?: error("The current video format class is missing")
}

private val currentVideoFormatConstructor = method("current video format constructor") {
    inClass(currentVideoFormatClass)
    flags(AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR)
    returns(Type.Void)
    opcode(Opcode.IPUT_OBJECT)
    hasParam("[" + videoQualityClass.descriptor)
}

private val qualityArrayPut = currentVideoFormatConstructor.point("current quality array") {
    opcode(Opcode.IPUT_OBJECT)
    field {
        owner(currentVideoFormatClass.descriptor)
        type(("[" + videoQualityClass.descriptor))
    }
}

val hidePremiumVideoQuality = patch("Hide Premium video quality") {
    description("Removes Premium-only quality choices from the quality menu.")
    compatibleWith(YOUTUBE)
    dependsOn(youTubeSettings, videoInformationHook)
    settings(youTubeSettings, section(YouTubeSettingsPages.Video, "Video quality", YouTubeSettings.hidePremiumVideoQuality))

    execute {
        // The manual extension filter keeps the app constructor's concrete array type and avoids
        // generating an ExternalSyntheticLambda that would need to be patched too.
        qualityArrayPut.captureAs("qualities", "[" + videoQualityClass.descriptor).before {
            val filtered = call(
                HidePremiumVideoQuality.hidePremiumVideoQuality,
                capture("qualities").cast(EXTENSION_VIDEO_QUALITY_ARRAY),
            )
            capture("qualities").assign(filtered.cast(("[" + videoQualityClass.descriptor)))
        }
    }
}
