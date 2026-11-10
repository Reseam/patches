// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.video

import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.YouTubePatchStrings
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.mainActivityOnCreate
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.patches.youtube.internal.videoInformationHook
import app.reseam.youtubecommon.settings.YouTubeCommonSettings
import app.reseam.patches.youtubecommon.preferOriginalAudio
import app.reseam.youtube.settings.YouTubeStrings

val forceOriginalAudio = patch(YouTubePatchStrings.forceOriginalAudio) {
    description(YouTubePatchStrings.forceOriginalAudioDescription)
    compatibleWith(YOUTUBE)
    dependsOn(youTubeSettings, videoInformationHook)
    settings(youTubeSettings, section(YouTubeSettingsPages.Video, YouTubeStrings.sectionAudio, YouTubeCommonSettings.forceOriginalAudio))

    execute { preferOriginalAudio(mainActivityOnCreate) }
}
