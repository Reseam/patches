// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.video.quality

import app.reseam.patch.ExtClass
import app.reseam.patch.Type
import app.reseam.patch.before
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.parameterTypes
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettings
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.patches.youtube.internal.onCreateHook
import app.reseam.patches.youtube.internal.setPlaybackSpeedMethod
import app.reseam.patches.youtube.internal.videoInformationHook
import app.reseam.patches.youtube.internal.videoQualityClass

object RememberVideoQuality : ExtClass("app.reseam.youtube.quality.RememberVideoQuality") {
    val newVideoStarted = static("newVideoStarted", "app.reseam.youtube.video.VideoInformation\$PlaybackController")
    val userChangedQuality = static("userChangedQuality", Type.Int)
    val userChangedShortsQuality = static("userChangedShortsQuality", Type.Int)
}

private val qualityMenuParent = method("video quality menu parent") {
    strings("VIDEO_QUALITIES_MENU_BOTTOM_SHEET_FRAGMENT")
    returns(Type.Void)
}

private val qualityMenuClass = app.reseam.patch.classTarget("video quality menu class") {
    bytecode.findClass(qualityMenuParent.owner) ?: error("The video quality menu class is missing")
}

private val qualityItemClick = method("video quality item click") {
    inClass(qualityMenuClass)
    name("onItemClick")
    flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
    returns(Type.Void)
    paramCount(4)
    param(2, Type.Int)
    param(3, Type.Long)
    custom { parameterTypes[0].startsWith("L") && parameterTypes[1].startsWith("L") }
}

private val qualitySetter = method("player quality setter") {
    inClass(klass(setPlaybackSpeedMethod.owner))
    params(videoQualityClass.descriptor)
    returns(Type.Void)
}

private val qualityChangedMethod = method("video quality command") {
    calls(qualitySetter)
    paramCount(1)
    custom { returnType.startsWith("L") }
}

private val qualityChangedValue = qualityChangedMethod.point("selected video quality") {
    invokeDirect { owner(videoQualityClass.descriptor); name("<init>") }
}.captureArgumentAs("quality", 1, Type.Int)

val rememberVideoQuality = patch("Remember video quality") {
    description("Remembers the selected video quality and applies the configured default.")
    compatibleWith(YOUTUBE)
    dependsOn(youTubeSettings, videoInformationHook)
    settings(youTubeSettings, section(YouTubeSettingsPages.Video, "Video quality", YouTubeSettings.videoQualityDefault, YouTubeSettings.rememberVideoQuality))

    execute {
        onCreateHook(RememberVideoQuality.newVideoStarted)
        qualityChangedValue.before { call(RememberVideoQuality.userChangedQuality, capture("quality")) }
        qualityItemClick.before { call(RememberVideoQuality.userChangedShortsQuality, param(2)) }
    }
}
