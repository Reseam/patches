// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.playerui;

import app.reseam.youtube.core.Logger;
import app.reseam.youtube.litho.Filter;
import app.reseam.youtube.litho.FilterGroup.StringFilterGroup;

public final class HideInfoCardsFilter extends Filter {
    public HideInfoCardsFilter() {
        addIdentifierCallbacks(new StringFilterGroup(
                "hide_info_cards", false, "info_card_teaser_overlay.e"));
        Logger.debug(() -> "HideInfoCardsFilter: info-card identifier filter registered");
    }
}
