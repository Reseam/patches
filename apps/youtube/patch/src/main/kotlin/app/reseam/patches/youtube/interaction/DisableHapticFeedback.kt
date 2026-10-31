// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.interaction

import app.reseam.patch.Type
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.youtube.settings.PlayerControlsSettings
import app.reseam.youtube.settings.YouTubeStrings

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
            YouTubeStrings.sectionHapticFeedback,
            PlayerControlsSettings.hapticsChapters,
            PlayerControlsSettings.hapticsPreciseSeeking,
            PlayerControlsSettings.hapticsSeekUndo,
            PlayerControlsSettings.hapticsZoom,
        ),
    )

    execute {
        gate(PlayerControlsSettings.hapticsChapters) {
            chapterHaptics.alwaysReturn()
        }
        gate(PlayerControlsSettings.hapticsPreciseSeeking) {
            preciseSeekingHaptics.alwaysReturn()
        }
        gate(PlayerControlsSettings.hapticsSeekUndo) {
            seekUndoHaptics.alwaysReturn()
        }
        gate(PlayerControlsSettings.hapticsZoom) {
            zoomHaptics.alwaysReturn()
        }
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
