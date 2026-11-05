// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.layout

import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.before
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.youtube.miniplayer.bindings.CompactMiniplayer
import app.reseam.youtube.settings.PlayerSettings
import app.reseam.youtube.settings.YouTubeStrings

val compactMiniplayer = patch("Compact miniplayer") {
    description("Adds an option to show the minimized video as a bar above the navigation bar.")
    compatibleWith(YOUTUBE)
    settings(youTubeSettings, section(YouTubeSettingsPages.Player, YouTubeStrings.sectionMiniplayer, PlayerSettings.compactMiniplayer))

    execute {
        watchLayout("onLayout").after { CompactMiniplayer.layout(thisObject) }
        watchLayout("onInterceptTouchEvent").before {
            whenTrue(CompactMiniplayer.ownsTouch(thisObject, param(0))) {
                returnValue(CompactMiniplayer.interceptTouch(thisObject, param(0)))
            }
        }
        watchLayout("onTouchEvent").before {
            whenTrue(CompactMiniplayer.ownsTouch(thisObject, param(0))) {
                returnValue(CompactMiniplayer.touch(thisObject, param(0)))
            }
        }
        videoMetadata.after { CompactMiniplayer.setMetadata(param(0), param(1)) }
        controlsVideoState.before { CompactMiniplayer.setVideoState(param(0)) }
    }
}

// Lays out the player, the miniplayer and the watch panel.
private fun watchLayout(name: String) = method("watch layout $name") {
    inClass(klass("com.google.android.apps.youtube.app.watch.nextgenwatch.ui.NextGenWatchLayout"))
    name(name)
}

// Title and channel of each player response, extracted for the miniplayer.
private val videoMetadata = method("miniplayer video metadata") {
    inClass(klass(method("miniplayer video metadata text") {
        strings("FloatyBarPlayerResponseExtractedModel{title=")
    }.owner))
    flags(AccessFlags.CONSTRUCTOR)
    params(Type.String, Type.String, Type.Boolean)
}

// Sets the video state the player controls are drawn for.
private val controlsVideoState = method("player controls video state") {
    strings("Null videoState")
    paramCount(1)
    returns(Type.Void)
}
