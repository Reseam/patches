// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.misc

import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettings
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.mainActivityOnCreate
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.patches.youtube.internal.ClientContextEndpoint
import app.reseam.patches.youtube.internal.clientContextHook
import app.reseam.patches.youtube.internal.hookBackgroundPlayVideoId
import app.reseam.patches.youtube.internal.hookClientContextOsName
import app.reseam.patches.youtube.internal.hookVideoId
import app.reseam.patches.youtube.internal.videoIdHook
import app.reseam.patches.youtubespoof.SpoofSettings
import app.reseam.patches.youtubespoof.SpoofVideoStreams
import app.reseam.patches.youtubespoof.spoofStreams

val spoofVideoStreams = patch("Spoof video streams") {
    description("Requests and installs playback streams from a compatible YouTube client.")
    compatibleWith(YOUTUBE)
    dependsOn(videoIdHook, clientContextHook)
    settings(
        youTubeSettings,
        section(
            YouTubeSettingsPages.Advanced,
            "Stream compatibility",
            SpoofSettings.spoofVideoStreams,
            SpoofSettings.spoofVideoStreamsClient,
            SpoofSettings.spoofVideoStreamsTokenSource,
            YouTubeSettings.spoofVideoStreamsStatsForNerds,
        ),
    )

    execute {
        spoofStreams(mainActivityOnCreate)

        // The active video selects the client shown in stats for nerds.
        hookVideoId(SpoofVideoStreams.onVideoChanged)
        hookBackgroundPlayVideoId(SpoofVideoStreams.onVideoChanged)
        nerdsStatsVideoFormatBuilder.after {
            capture("result").assign(call(SpoofVideoStreams.appendSpoofedClient, capture("result")))
        }

        hookClientContextOsName(ClientContextEndpoint.BROWSE, SpoofVideoStreams.rewriteClientContextOsName)
        hookClientContextOsName(ClientContextEndpoint.SEARCH, SpoofVideoStreams.rewriteClientContextOsName)
        hookClientContextOsName(ClientContextEndpoint.REEL, SpoofVideoStreams.rewriteClientContextOsName)
    }
}

private val nerdsStatsVideoFormatBuilder = method("stats for nerds video format builder") {
    flags(AccessFlags.PUBLIC or AccessFlags.STATIC)
    paramCount(1)
    returns(Type.String)
    strings("codecs=\"")
}
