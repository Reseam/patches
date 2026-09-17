// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.player

import app.reseam.patch.ExtClass
import app.reseam.patch.Type
import app.reseam.patch.before
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettings
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings

val disableAutoCaptions = patch("Disable auto captions") {
    description("Prevents captions from being enabled automatically.")
    compatibleWith(YOUTUBE)
    dependsOn(youTubeSettings)
    settings(youTubeSettings, section(YouTubeSettingsPages.Overlay, "Player overlay", YouTubeSettings.disableAutoCaptions))

    execute {
        val subtitleTrack = method("subtitle track option") {
            flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
            params()
            returns(Type.Boolean)
            strings("DISABLE_CAPTIONS_OPTION")
        }
        subtitleTrack.before {
            whenTrue(call(DisableAutoCaptions.disableAutoCaptions)) { returnTrue() }
        }

        val pcStart = method("start video informer") {
            strings("pc")
            paramCount(4)
            returns(Type.Void)
        }
        pcStart.before { call(DisableAutoCaptions.setCaptionsButtonStatus, bool(false)) }

        subtitleButtonController.before { call(DisableAutoCaptions.setCaptionsButtonStatus, bool(true)) }
    }
}

object DisableAutoCaptions : ExtClass("app.reseam.youtube.playerui.DisableAutoCaptions") {
    val disableAutoCaptions = static("disableAutoCaptions", returns = Type.Boolean)
    val setCaptionsButtonStatus = static("setCaptionsButtonStatus", Type.Boolean)
}
