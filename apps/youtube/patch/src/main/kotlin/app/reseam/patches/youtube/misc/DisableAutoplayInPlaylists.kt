// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.misc

import app.reseam.patch.Type
import app.reseam.patch.before
import app.reseam.patch.enumConstant
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.youtube.settings.PlayerSettings
import app.reseam.youtube.settings.YouTubeStrings

// The playback sequencer's own enum shares these names and adds START and RETRY.
private val navigationDirection = klass("navigation direction") {
    enumConstants("NEXT", "PREVIOUS", "AUTOPLAY", "AUTONAV", "JUMP", "INSERT")
    custom { staticFields.count { it.fieldType == descriptor } == 6 }
}

private val autoplayDirection = navigationDirection.enumConstant("AUTOPLAY")

/** How a playlist or queue answers whether it can move in a direction. */
private val listNavigationAvailability = method("list navigation availability") {
    returns(Type.Int)
    paramCount(1)
    accesses { owner(autoplayDirection.owner); name(autoplayDirection.name) }
}

// 1 is what a list answers when it has no item in that direction.
private const val UNAVAILABLE = 1

val disableAutoplayInPlaylists = patch("Disable autoplay in playlists") {
    description("Stops at the end of each video in a playlist instead of playing the next one.")
    compatibleWith(YOUTUBE)
    dependsOn(youTubeSettings)
    settings(youTubeSettings, section(YouTubeSettingsPages.Player, YouTubeStrings.sectionPlayer, PlayerSettings.disableAutoplayInPlaylists))

    execute {
        gate(PlayerSettings.disableAutoplayInPlaylists) {
            listNavigationAvailability.before {
                whenEqual(param(0).fieldOfType(navigationDirection.descriptor), staticField(autoplayDirection)) {
                    returnValue(int(UNAVAILABLE))
                }
            }
        }
    }
}
