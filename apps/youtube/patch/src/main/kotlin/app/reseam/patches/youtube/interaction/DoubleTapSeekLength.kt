// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.interaction

import app.reseam.patch.before
import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.YouTubePatchStrings
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.youtube.settings.PlayerControlsSettings
import app.reseam.youtube.settings.YouTubeStrings
import app.reseam.youtube.swipe.bindings.DoubleTapSeek

private const val DURATION = "j\$.time.Duration"

val doubleTapSeekLength = patch(YouTubePatchStrings.doubleTapSeekLength) {
    description(YouTubePatchStrings.doubleTapSeekLengthDescription)
    compatibleWith(YOUTUBE)
    settings(youTubeSettings, section(YouTubeSettingsPages.Controls, YouTubeStrings.sectionDoubleTap,
        PlayerControlsSettings.doubleTapSeekForward, PlayerControlsSettings.doubleTapSeekBack))

    execute {
        // Every double tap builds this record with the tapped side and YouTube's length, negated for the left side.
        doubleTapInfoConstructor.before {
            val millis = DoubleTapSeek.seekMillis(param(1), param(3).callVirtual(DURATION, "toMillis", "()J"))
            param(3).assign(callStatic(DURATION, "ofMillis", "(J)L${DURATION.replace('.', '/')};", millis))
        }
    }
}
