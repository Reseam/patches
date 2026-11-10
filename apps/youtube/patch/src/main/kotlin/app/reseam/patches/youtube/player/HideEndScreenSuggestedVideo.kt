// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.player

import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.before
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.Opcode
import app.reseam.patch.dex.opcode
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.YouTubePatchStrings
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.youtube.playerui.bindings.HideEndScreenSuggestedVideo
import app.reseam.youtube.settings.PlayerOverlaySettings
import app.reseam.youtube.settings.YouTubeStrings

val hideEndScreenSuggestedVideo = patch(YouTubePatchStrings.hideEndScreenSuggestedVideo) {
    description(YouTubePatchStrings.hideEndScreenSuggestedVideoDescription)
    compatibleWith(YOUTUBE)
    dependsOn(youTubeSettings)
    settings(youTubeSettings, section(YouTubeSettingsPages.Overlay, YouTubeStrings.sectionEndScreen, PlayerOverlaySettings.hideEndScreenSuggestedVideo))

    execute {
        val autoNavConstructor = method("autoplay constructor") {
            flags(AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR)
            returns(Type.Void)
            strings("main_app_autonav")
        }
        val autoNavClass = app.reseam.patch.classTarget("autoplay class") {
            bytecode.findClass(autoNavConstructor.owner)
                ?: error("The autoplay class is missing")
        }
        val autoNavStatus = method("autoplay status") {
            inClass(autoNavClass)
            flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
            params()
            returns(Type.Boolean)
        }

        autoNavStatus.after {
            HideEndScreenSuggestedVideo.setAutoplayStatus(result)
        }

        val endScreenName = method("autonav end-screen overlay name") {
            params()
            returns(Type.String)
            strings("player_overlay_player_autonav_endscreen")
            opcode(Opcode.CONST_STRING, Opcode.RETURN_OBJECT)
        }
        val endScreenClass = app.reseam.patch.classTarget("autonav end-screen overlay") {
            bytecode.findClass(endScreenName.owner)
                ?: error("The autonav end-screen overlay is missing")
        }
        method("autonav end-screen overlay visible") {
            inClass(endScreenClass)
            flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
            params()
            returns(Type.Boolean)
        }.before {
            whenTrue(HideEndScreenSuggestedVideo.shouldHide()) { returnFalse() }
        }
    }
}
