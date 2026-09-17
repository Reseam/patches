// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.quality;

import app.reseam.youtube.litho.Filter;
import app.reseam.youtube.litho.FilterGroup.StringFilterGroup;

/** Keeps the quick quality component visible while the menu opener watches its view tree. */
public final class AdvancedVideoQualityFilter extends Filter {
    public AdvancedVideoQualityFilter() {
        addPathCallbacks(new StringFilterGroup(
                "advanced_video_quality_menu", true, "quick_quality_sheet_content.e"));
    }

    @Override
    public boolean isFiltered(String identifier, String accessibility, String path, byte[] buffer,
                              StringFilterGroup matchedGroup, FilterContentType contentType,
                              int contentIndex) {
        AdvancedVideoQualityMenu.markQuickMenuVisible();
        return false;
    }
}
