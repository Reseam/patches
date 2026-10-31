// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtubemusic.layout

import app.reseam.runtime.Export
import app.reseam.youtube.litho.Filter
import app.reseam.youtube.litho.FilterGroup.StringFilterGroup
import app.reseam.youtubemusic.settings.YouTubeMusicSettings

@Export
class PremiumPromotionsFilter : Filter() {
    init {
        addIdentifierCallbacks(
            StringFilterGroup(
                YouTubeMusicSettings.hidePremiumPromotions,
                "statement_banner",
                "music_compact_banner.e",
                "alert_banner_promo.e",
                "music_paid_content_overlay.e",
            )
        )
    }
}
