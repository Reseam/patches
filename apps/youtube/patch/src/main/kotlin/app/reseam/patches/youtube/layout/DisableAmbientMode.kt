// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.layout

import app.reseam.patch.Type
import app.reseam.patch.classTarget
import app.reseam.patch.dex.parameterTypes
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.YouTubePatchStrings
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.youtube.settings.PlayerSettings
import app.reseam.youtube.settings.YouTubeStrings

val disableAmbientMode = patch(YouTubePatchStrings.disableAmbientMode) {
    description(YouTubePatchStrings.disableAmbientModeDescription)
    compatibleWith(YOUTUBE)
    settings(youTubeSettings, section(YouTubeSettingsPages.Player, YouTubeStrings.sectionAmbientMode, PlayerSettings.disableAmbientMode))

    execute {
        val optional = cinematicRendererState.parameterTypes[1]
        gate(PlayerSettings.disableAmbientMode) {
            // Without the watch response's cinematic container there is nothing to light the page with.
            cinematicRendererState.before {
                param(1).assign(callStatic(optional, "empty", "()$optional"))
            }
        }
    }
}

private val rendererStateToString = method("cinematic renderer state toString") {
    name("toString")
    returns(Type.String)
    strings("RendererWithState{rendererLoadedState=")
}

private val rendererStateClass = classTarget("cinematic renderer state") {
    bytecode.findClass(rendererStateToString.owner) ?: error("The cinematic renderer state class is missing")
}

private val cinematicRendererState = method("cinematic renderer state constructor") {
    inClass(rendererStateClass)
    name("<init>")
    paramCount(2)
}
