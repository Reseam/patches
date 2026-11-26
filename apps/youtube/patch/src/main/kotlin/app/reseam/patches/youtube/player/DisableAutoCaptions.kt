// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.player

import app.reseam.patch.Type
import app.reseam.patch.before
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.klass
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
        pcStart.before { DisableAutoCaptions.setUserChoseTrack(bool(false)) }

        // The captions button redraws for every track change, YouTube's own included, so only a
        // press of the button or a choice in the captions menu counts as the user turning captions
        // on. The press is caught before the button reads the current track, which the override
        // would otherwise report as off.
        val captionsButtonPress = method("captions button press") {
            inClass(klass(subtitleButtonController.method.owner))
            params(Type.Boolean)
            returns(Type.Void)
            calls { name("<init>"); params(Type.Object, Type.Boolean, Type.Int) }
        }
        captionsButtonPress.before { DisableAutoCaptions.setUserChoseTrack(bool(true)) }

        val trackSave = method("subtitle track save") {
            returns(Type.Void)
            paramCount(3)
            param(0, subtitleTrack.method.owner)
            param(2, Type.Int)
            calls(subtitleTrack)
        }
        val userTrackChoice = method("user subtitle track choice") {
            returns(Type.Void)
            params(subtitleTrack.method.owner, trackSave.method.parameterTypes[1])
            literals(USER_TRACK_CHOICE)
            calls(trackSave)
        }
        userTrackChoice.before { DisableAutoCaptions.setUserChoseTrack(bool(true)) }
    }
}

private const val USER_TRACK_CHOICE = 4L
