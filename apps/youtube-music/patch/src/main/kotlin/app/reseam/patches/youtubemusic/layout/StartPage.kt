// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtubemusic.layout

import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patch.settings.valueOf
import app.reseam.patches.youtubemusic.YouTubeMusicPatchStrings
import app.reseam.patches.youtubemusic.core.YOUTUBE_MUSIC
import app.reseam.youtubemusic.settings.YouTubeMusicSettings
import app.reseam.patches.youtubemusic.core.YouTubeMusicSettingsPages
import app.reseam.patches.youtubemusic.core.youTubeMusicSettings
import app.reseam.youtubemusic.settings.YouTubeMusicStrings

val startPage = patch(YouTubeMusicPatchStrings.startPage) {
    description(YouTubeMusicPatchStrings.startPageDescription)
    compatibleWith(YOUTUBE_MUSIC)
    settings(youTubeMusicSettings, section(YouTubeMusicSettingsPages.Interface, YouTubeMusicStrings.sectionStartPage, YouTubeMusicSettings.startPage))

    execute {
        defaultBrowseId.after {
            val page = valueOf(YouTubeMusicSettings.startPage)
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
