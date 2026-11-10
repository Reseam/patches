// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.video

import app.reseam.patch.Type
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.YouTubePatchStrings
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.youtube.settings.VideoSettings
import app.reseam.youtube.settings.YouTubeStrings

// The resume logs this visual element when it restarts playback. 20.26 has no resume on return.
private val resumeOnReturn = methods("resume playback on return") {
    literals(0x4ce47L)
    returns(Type.Void)
}

val disableResumingPlaybackOnReturn = patch(YouTubePatchStrings.disableResumingPlaybackOnReturn) {
    description(YouTubePatchStrings.disableResumingPlaybackOnReturnDescription)
    compatibleWith(YOUTUBE)
    dependsOn(youTubeSettings)
    settings(
        youTubeSettings,
        section(YouTubeSettingsPages.Video, YouTubeStrings.sectionBackgroundPlayback, VideoSettings.disableResumingPlaybackOnReturn),
    )

    execute {
        gate(VideoSettings.disableResumingPlaybackOnReturn) {
            resumeOnReturn.all.forEach { it.alwaysReturn() }
        }
    }
}
