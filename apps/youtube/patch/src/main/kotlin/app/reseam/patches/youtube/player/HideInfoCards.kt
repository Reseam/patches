// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.player

import app.reseam.patch.ExtClass
import app.reseam.patch.Type
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.Opcode
import app.reseam.patch.dex.opcode
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.after
import app.reseam.patch.settings.section
import app.reseam.patch.settings.skipWhen
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettings
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.patches.youtube.internal.lithoFilter
import app.reseam.patches.youtube.internal.registerLithoFilter

val hideInfoCards = patch("Hide info cards") {
    description("Removes information cards shown over videos.")
    compatibleWith(YOUTUBE)
    dependsOn(youTubeSettings, lithoFilter)
    settings(youTubeSettings, section(YouTubeSettingsPages.Overlay, "Player overlay", YouTubeSettings.hideInfoCards))

    execute {
        val drawerId = resources.id("id", "info_cards_drawer_header")?.toLong()
            ?: error("id/info_cards_drawer_header is missing")
        val incognitoParent = method("info-card incognito parent") {
            flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
            returns(Type.String)
            strings("player_overlay_info_card_teaser")
        }
        val incognitoOwner = app.reseam.patch.classTarget("info-card incognito class") {
            bytecode.findClass(incognitoParent.owner) ?: error("The info-card class is missing")
        }
        val incognito = method("info-card incognito renderer") {
            inClass(incognitoOwner)
            flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
            paramCount(2)
            param(1, Type.Long)
            strings("vibrator")
            returns("java.lang.Boolean")
            custom { parameterTypes[0].startsWith("L") }
        }
        incognito.point("info-card visibility") {
            invokeVirtual {
                owner("android.view.View")
                name("setVisibility")
                params(Type.Int)
                returns(Type.Void)
            }
        }.captureArgumentAs("view", 0, Type.View).after(YouTubeSettings.hideInfoCards) {
            capture("view").callVirtual(Type.View, "setVisibility", "(I)V", int(8))
        }

        val oldInfoCardsMethod = method("old info-card method call") {
            strings("Missing ControlsOverlayPresenter for InfoCards to work.")
            literals(drawerId)
            opcode(Opcode.INVOKE_VIRTUAL, Opcode.IGET_OBJECT, Opcode.INVOKE_INTERFACE)
        }
        oldInfoCardsMethod.point("old info-card interface call") {
            opcode(Opcode.IGET_OBJECT)
            then {
                invokeInterface {
                    params()
                    returns(Type.Void)
                }
            }
        }.skipWhen(YouTubeSettings.hideInfoCards)

        registerLithoFilter(HideInfoCardsFilter)
    }
}

object HideInfoCardsFilter : ExtClass("app.reseam.youtube.playerui.HideInfoCardsFilter")
