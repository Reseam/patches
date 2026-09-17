// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.misc

import app.reseam.patch.ExtClass
import app.reseam.patch.Type
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.returnFalseWhen
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettings
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings

val disableVideoCodecs = patch("Disable video codecs") {
    description("Turns off HDR and the VP9 codec, which lowers battery use at the cost of quality.")
    compatibleWith(YOUTUBE)
    settings(
        youTubeSettings,
        section(YouTubeSettingsPages.Video, "Codecs", YouTubeSettings.disableHdrVideo, YouTubeSettings.forceAvcCodec),
    )

    execute {
        // HDR is decided from the display's own capability list, wherever the app asks for it.
        bytecode.redirectCalls("android.view.Display\$HdrCapabilities", "getSupportedHdrTypes", VideoCodecs.supportedHdrTypes)
        vp9Supported.returnFalseWhen(YouTubeSettings.forceAvcCodec)
    }
}

// One capability check names both the flag and the mime type it decides.
val vp9Supported = method("vp9Supported") {
    strings("vp9_supported", "video/x-vnd.on2.vp9")
    returns(Type.Boolean)
}

object VideoCodecs : ExtClass("app.reseam.youtube.misc.VideoCodecs") {
    val supportedHdrTypes = static("supportedHdrTypes", "android.view.Display\$HdrCapabilities", returns = "[I")
}
