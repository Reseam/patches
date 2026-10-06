// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.video

import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.mainActivityOnCreate
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.patches.youtube.internal.videoInformationHook
import app.reseam.patches.youtubecommon.YouTubeCommonSettings
import app.reseam.patches.youtubecommon.preferOriginalAudio

val forceOriginalAudio = patch("Force original audio") {
    description("Prefers the original audio stream when YouTube offers a dubbed track.")
    compatibleWith(YOUTUBE)
    dependsOn(youTubeSettings, videoInformationHook)
    settings(youTubeSettings, section(YouTubeSettingsPages.Video, "Audio", YouTubeCommonSettings.forceOriginalAudio))

    execute { preferOriginalAudio(mainActivityOnCreate) }
}
