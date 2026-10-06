// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtubemusic.layout;

import static app.reseam.youtubemusic.core.MusicSettings.toggle;

import app.reseam.youtube.litho.Filter;
import app.reseam.youtube.litho.FilterGroup.StringFilterGroup;

public final class LayoutComponentsFilter extends Filter {
    public LayoutComponentsFilter() {
        addIdentifierCallbacks(new StringFilterGroup("hide_speed_dial", toggle("hide_speed_dial", false), "music_speed_dial_shelf.e"));
    }
}
