// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtubemusic.layout;

import static app.reseam.youtubemusic.core.MusicSettings.toggle;

import app.reseam.youtube.litho.Filter;
import app.reseam.youtube.litho.FilterGroup.StringFilterGroup;

public final class PremiumPromotionsFilter extends Filter {
    public PremiumPromotionsFilter() {
        addIdentifierCallbacks(new StringFilterGroup("hide_premium_promotions", toggle("hide_premium_promotions", true),
                "statement_banner", "music_compact_banner.e", "alert_banner_promo.e", "music_paid_content_overlay.e"));
    }
}
