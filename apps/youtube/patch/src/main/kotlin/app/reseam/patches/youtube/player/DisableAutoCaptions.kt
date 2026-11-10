// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.player

import app.reseam.patch.Type
import app.reseam.patch.before
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.YouTubePatchStrings
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.youtube.playerui.bindings.DisableAutoCaptions
import app.reseam.youtube.settings.PlayerOverlaySettings
import app.reseam.youtube.settings.YouTubeStrings

val disableAutoCaptions = patch(YouTubePatchStrings.disableAutoCaptions) {
    description(YouTubePatchStrings.disableAutoCaptionsDescription)
    compatibleWith(YOUTUBE)
    dependsOn(youTubeSettings)
    settings(youTubeSettings, section(YouTubeSettingsPages.Overlay, YouTubeStrings.sectionPlayerOverlay, PlayerOverlaySettings.disableAutoCaptions))

    execute {
        val subtitleTrack = method("subtitle track option") {
            flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
            params()
            returns(Type.Boolean)
            strings("DISABLE_CAPTIONS_OPTION")
        }
        subtitleTrack.before {
            whenTrue(DisableAutoCaptions.disableAutoCaptions()) { returnTrue() }
        }

        val pcStart = method("start video informer") {
            strings("pc")
            paramCount(4)
            returns(Type.Void)
        }
        pcStart.before { DisableAutoCaptions.setCaptionsButtonStatus(bool(false)) }

        subtitleButtonController.before { DisableAutoCaptions.setCaptionsButtonStatus(bool(true)) }
    }
}
