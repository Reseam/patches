// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.interaction

import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patch.settings.whenEnabled
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.patches.youtube.internal.booleanFeatureReads
import app.reseam.youtube.settings.VideoSettings
import app.reseam.youtube.settings.YouTubeStrings

private const val DISABLE_PLAY_QUEUE_DIALOG = 45414906L
private const val DISCONNECT_ON_VIDEO_CHANGE = 45693138L

val restoreCastQueue = patch("Restore Cast Play/Queue dialog") {
    description("Restores YouTube's Play/Queue dialog and keeps the Cast session when selecting another video.")
    compatibleWith(YOUTUBE)
    dependsOn(youTubeSettings)
    settings(
        youTubeSettings,
        section(YouTubeSettingsPages.Video, YouTubeStrings.sectionQueue, VideoSettings.restoreCastQueue),
    )

    execute {
        listOf(DISABLE_PLAY_QUEUE_DIALOG, DISCONNECT_ON_VIDEO_CHANGE)
            .flatMap(::booleanFeatureReads)
            .forEach { site ->
                site.next { resultOf(Type.Boolean) }.captureAs("enabled", Type.Boolean).after {
                    whenEnabled(VideoSettings.restoreCastQueue) { capture("enabled").assign(bool(false)) }
                }
            }
    }
}
