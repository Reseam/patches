// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtubemusic.layout

import app.reseam.patch.ExtClass
import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.youtubemusic.core.YOUTUBE_MUSIC
import app.reseam.patches.youtubemusic.core.YouTubeMusicSettings
import app.reseam.patches.youtubemusic.core.YouTubeMusicSettingsPages
import app.reseam.patches.youtubemusic.core.youTubeMusicSettings

val startPage = patch("Start page") {
    description("Opens a page of your choice instead of Home.")
    compatibleWith(YOUTUBE_MUSIC)
    settings(youTubeMusicSettings, section(YouTubeMusicSettingsPages.Interface, "Start page", YouTubeMusicSettings.startPage))

    execute {
        defaultBrowseId.after {
            val page = call(Settings.getChoice, string(YouTubeMusicSettings.startPage.key))
            whenFalse(page.callVirtual("java.lang.String", "isEmpty", "()Z")) { returnValue(page) }
        }
    }
}

// The page the app opens at launch and returns to at the root. Signed out, it is the device's own
// music; otherwise the server's choice, or Home.
private val defaultBrowseId = method("defaultBrowseId") {
    strings("FEmusic_library_sideloaded_tracks", "FEmusic_home")
    params()
    returns(Type.String)
}

private object Settings : ExtClass("app.reseam.runtime.settings.ReseamSettings") {
    val getChoice by static(Type.String, returns = Type.String)
}
