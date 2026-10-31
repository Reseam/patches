// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.player

import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.before
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.Opcode
import app.reseam.patch.dex.opcode
import app.reseam.patch.dex.returnType
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.youtube.playerui.bindings.HideEndScreenSuggestedVideo
import app.reseam.youtube.settings.PlayerOverlaySettings
import app.reseam.youtube.settings.YouTubeStrings

val hideEndScreenSuggestedVideo = patch("Hide end-screen suggested video") {
    description("Removes the suggested video shown at the end of playback.")
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

        val removeListener = method("remove end-screen layout listener") {
            flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
            params()
            returns(Type.Void)
            opcode(Opcode.IPUT, Opcode.INVOKE_VIRTUAL)
            calls { name("removeOnLayoutChangeListener"); returns(Type.Void) }
            callsMethod {
                name == "removeOnLayoutChangeListener" &&
                    definingClass.endsWith("/YouTubePlayerOverlaysLayout;") &&
                    returnType == Type.Void
            }
        }
        removeListener.before {
            whenTrue(HideEndScreenSuggestedVideo.shouldHide()) { returnVoid() }
        }
    }
}
