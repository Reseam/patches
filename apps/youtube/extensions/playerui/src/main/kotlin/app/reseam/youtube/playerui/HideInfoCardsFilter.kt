// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.playerui

import app.reseam.runtime.Export
import app.reseam.youtube.litho.Filter
import app.reseam.youtube.litho.FilterGroup.StringFilterGroup
import app.reseam.youtube.settings.PlayerOverlaySettings

@Export
class HideInfoCardsFilter : Filter() {
    init {
        addIdentifierCallbacks(
            StringFilterGroup(PlayerOverlaySettings.hideInfoCards, "info_card_teaser_overlay.e")
        )
    }
}
