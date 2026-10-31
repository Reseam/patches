// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtubemusic.layout

import app.reseam.runtime.Export
import app.reseam.youtube.litho.Filter
import app.reseam.youtube.litho.FilterGroup.StringFilterGroup
import app.reseam.youtubemusic.settings.YouTubeMusicSettings

@Export
class LayoutComponentsFilter : Filter() {
    init {
        addIdentifierCallbacks(
            StringFilterGroup(YouTubeMusicSettings.hideSpeedDial, "music_speed_dial_shelf.e")
        )
    }
}
