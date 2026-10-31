// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.speed

import app.reseam.runtime.Export
import app.reseam.youtube.litho.Filter
import app.reseam.youtube.litho.FilterGroup.StringFilterGroup
import app.reseam.youtube.litho.LithoComponent
import app.reseam.youtube.settings.VideoSettings

@Export
class PlaybackSpeedMenuFilter : Filter() {
    init {
        addPathCallbacks(*PlaybackMenu.entries.map(::MenuGroup).toTypedArray())
    }

    override fun isFiltered(component: LithoComponent, group: StringFilterGroup): Boolean {
        (group as MenuGroup).menu.visible = true
        return false
    }
}

private class MenuGroup(val menu: PlaybackMenu) :
    StringFilterGroup(VideoSettings.customPlaybackSpeedMenu, menu.pattern)
