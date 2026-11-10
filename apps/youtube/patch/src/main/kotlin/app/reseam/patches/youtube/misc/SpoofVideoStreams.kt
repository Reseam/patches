// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.misc

import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.YouTubePatchStrings
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.mainActivityOnCreate
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.patches.youtube.internal.ClientContextEndpoint
import app.reseam.patches.youtube.internal.clientContextHook
import app.reseam.patches.youtube.internal.hookBackgroundPlayVideoId
import app.reseam.patches.youtube.internal.hookClientContextOsName
import app.reseam.patches.youtube.internal.hookVideoId
import app.reseam.patches.youtube.internal.videoIdHook
import app.reseam.patches.youtubespoof.spoofStreams
import app.reseam.youtube.settings.YouTubeStrings
import app.reseam.youtube.spoof.bindings.SpoofVideoStreams
import app.reseam.youtubespoof.settings.SpoofSettings

val spoofVideoStreams = patch(YouTubePatchStrings.spoofVideoStreams) {
    description(YouTubePatchStrings.spoofVideoStreamsDescription)
    compatibleWith(YOUTUBE)
    dependsOn(videoIdHook, clientContextHook)
    settings(
        youTubeSettings,
        section(
            YouTubeSettingsPages.Advanced,
            YouTubeStrings.sectionStreamCompatibility,
            SpoofSettings.spoofVideoStreams,
            SpoofSettings.spoofVideoStreamsClient,
            SpoofSettings.spoofVideoStreamsStatsForNerds,
        ),
    )

    execute {
        spoofStreams(mainActivityOnCreate)

        // The active video selects the client shown in stats for nerds.
        hookVideoId(SpoofVideoStreams.onVideoChanged)
        hookBackgroundPlayVideoId(SpoofVideoStreams.onVideoChanged)
        nerdsStatsVideoFormatBuilder.after {
            result.assign(SpoofVideoStreams.appendSpoofedClient(result))
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
    strings("codecs=\"", " otf")
}
