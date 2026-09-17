// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.interaction

import app.reseam.patch.Type
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patch.settings.skipWhen
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettings
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings

/**
 * Each player gesture vibrates from its own method, and each of those names the failure it logs
 * when the vibrator refuses. Returning early is the whole patch.
 *
 * The tap-and-hold vibration is not covered: it lives in one arm of a shared dispatch method that
 * hundreds of unrelated callbacks also run through, so returning early there is not an option.
 */
val disableHapticFeedback = patch("Disable haptic feedback") {
    description("Turns off the player's vibration for chapters, seeking and zoom.")
    compatibleWith(YOUTUBE)
    settings(
        youTubeSettings,
        section(
            YouTubeSettingsPages.Controls,
            "Haptic feedback",
            YouTubeSettings.hapticsChapters,
            YouTubeSettings.hapticsPreciseSeeking,
            YouTubeSettings.hapticsSeekUndo,
            YouTubeSettings.hapticsZoom,
        ),
    )

    execute {
        chapterHaptics.skipWhen(YouTubeSettings.hapticsChapters)
        preciseSeekingHaptics.skipWhen(YouTubeSettings.hapticsPreciseSeeking)
        seekUndoHaptics.skipWhen(YouTubeSettings.hapticsSeekUndo)
        zoomHaptics.skipWhen(YouTubeSettings.hapticsZoom)
    }
}

val chapterHaptics = method("chapterHaptics") {
    strings("Failed to execute markers haptics vibrate.")
    returns(Type.Void)
}

val preciseSeekingHaptics = method("preciseSeekingHaptics") {
    strings("Failed to haptics vibrate for fine scrubbing.")
    returns(Type.Void)
}

val seekUndoHaptics = method("seekUndoHaptics") {
    strings("Failed to execute seek undo haptics vibrate.")
    returns(Type.Void)
}

val zoomHaptics = method("zoomHaptics") {
    strings("Failed to haptics vibrate for video zoom")
    returns(Type.Void)
}
