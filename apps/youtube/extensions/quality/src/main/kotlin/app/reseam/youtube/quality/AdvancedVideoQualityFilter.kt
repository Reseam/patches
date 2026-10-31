// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.quality

import app.reseam.runtime.Export
import app.reseam.youtube.litho.Filter
import app.reseam.youtube.litho.FilterGroup.StringFilterGroup
import app.reseam.youtube.litho.LithoComponent
import app.reseam.youtube.settings.VideoSettings

@Export
class AdvancedVideoQualityFilter : Filter() {
    init {
        addPathCallbacks(
            StringFilterGroup(
                VideoSettings.advancedVideoQualityMenu,
                "quick_quality_sheet_content.e",
            )
        )
    }

    override fun isFiltered(component: LithoComponent, group: StringFilterGroup): Boolean {
        markQuickMenuVisible()
        return false
    }
}
