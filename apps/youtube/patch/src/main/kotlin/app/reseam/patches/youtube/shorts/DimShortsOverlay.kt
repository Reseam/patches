// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.shorts

import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.patches.youtube.internal.playerTypeHook
import app.reseam.patches.youtube.internal.toolbarButtonIcon
import app.reseam.patches.youtube.internal.toolbarButtonIdentified
import app.reseam.youtube.settings.ShortsSettings
import app.reseam.youtube.settings.YouTubeStrings
import app.reseam.youtube.shorts.bindings.DimShortsOverlay

val dimShortsOverlay = patch("Dim Shorts overlay") {
    description("Dims the Shorts overlay and toolbar, with an optional immersive status bar.")
    compatibleWith(YOUTUBE)
    dependsOn(playerTypeHook, youTubeSettings)
    settings(
        youTubeSettings,
        section(
            YouTubeSettingsPages.Shorts,
            YouTubeStrings.sectionShorts,
            ShortsSettings.dimShortsOverlayOpacity,
            ShortsSettings.dimShortsOverlayImmersiveMode,
        ),
    )

    execute {
        val reelWatchPlayer = resources.id("id", "reel_watch_player")?.toLong()
            ?: error("id/reel_watch_player is missing")
        method("shortsOverlayView") {
            literals(reelWatchPlayer)
            returns(Type.View)
        }.point {
            invokeVirtual { name("findViewById"); params(Type.Int); returns(Type.View) }
            argument(1) { literal(reelWatchPlayer) }
        }.next { resultOf(Type.View) }
            .captureAs("shortsOverlay", Type.View)
            .after { DimShortsOverlay.dimShortsPlayerOverlay(capture("shortsOverlay")) }

        // The existing toolbar seam captures the enum used to identify each button and follows
        // the branch join to the ImageView field.
        toolbarButtonIdentified.after {
            DimShortsOverlay.dimShortsToolbarButton(capture("toolbarButton"), thisObject.field(toolbarButtonIcon))
        }
    }
}
