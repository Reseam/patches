// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.misc

import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.YouTubePatchStrings
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.youtube.core.bindings.DisplayHdr
import app.reseam.youtube.misc.bindings.VideoCodecs
import app.reseam.youtube.settings.VideoSettings
import app.reseam.youtube.settings.YouTubeStrings
import app.reseam.youtubecommon.settings.YouTubeCommonSettings

val disableVideoCodecs = patch(YouTubePatchStrings.disableVideoCodecs) {
    description(YouTubePatchStrings.disableVideoCodecsDescription)
    compatibleWith(YOUTUBE)
    settings(
        youTubeSettings,
        section(
            YouTubeSettingsPages.Video,
            YouTubeStrings.sectionCodecs,
            YouTubeCommonSettings.disableHdrVideo,
            YouTubeCommonSettings.forceVp9Codec,
            VideoSettings.forceAvcCodec,
        ),
    )

    execute {
        // HDR is decided from the display's own capability list, wherever the app asks for it.
        bytecode.redirectCalls("android.view.Display\$HdrCapabilities", "getSupportedHdrTypes", DisplayHdr.supportedHdrTypes)
        codecSupported.after {
            result.assign(VideoCodecs.codecSupported(param(1), result))
        }
        gate(VideoSettings.forceAvcCodec) {
            vp9Supported.alwaysReturn(false)
        }
    }
}

// One capability check names both the flag and the mime type it decides.
val vp9Supported = method("vp9Supported") {
    strings("vp9_supported", "video/x-vnd.on2.vp9")
    returns(Type.Boolean)
}

// Every codec check, AV1 included, asks this cached decoder lookup with a flag name and a mime type.
private val codecSupported = method("codecSupported") {
    calledBy(vp9Supported)
    paramCount(7)
    param(0, Type.String)
    param(1, Type.String)
    returns(Type.Boolean)
}
