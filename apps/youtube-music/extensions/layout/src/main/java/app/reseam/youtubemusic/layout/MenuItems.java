// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtubemusic.layout;

import static app.reseam.youtubemusic.core.MusicSettings.toggle;

import android.view.View;
import android.view.ViewGroup;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;

/** Rows of the long-press and three-dot menus, identified by the icon the server gives each one. */
public final class MenuItems {
    private static final Map<String, BooleanSupplier> HIDDEN = new HashMap<>();

    static {
        hide("hide_menu_start_mix", "MIX");
        hide("hide_menu_add_to_queue", "QUEUE_MUSIC");
        hide("hide_menu_library", "BOOKMARK", "BOOKMARK_BORDER", "LIBRARY_ADD", "LIBRARY_REMOVE");
        hide("hide_menu_go_to_album", "ALBUM");
        hide("hide_menu_go_to_artist", "ARTIST");
        hide("hide_menu_song_credits", "PEOPLE_GROUP");
        hide("hide_menu_speed_dial", "PIN_OUTLINE", "PIN_OFF_OUTLINE", "KEEP", "KEEP_OFF");
        hide("hide_menu_not_interested", "HIDE");
        hide("hide_menu_dont_recommend_artist", "PERSON_CIRCLE_SLASH");
        hide("hide_menu_dismiss_queue", "DISMISS_QUEUE");
        hide("hide_menu_report", "FLAG");
        hide("hide_menu_quality", "SETTINGS_MATERIAL");
        hide("hide_menu_captions", "CAPTIONS");
        hide("hide_menu_sleep_timer", "MOON_Z");
    }

    private MenuItems() {}

    private static void hide(String setting, String... icons) {
        BooleanSupplier enabled = toggle(setting, false);
        for (String icon : icons) HIDDEN.put(icon, enabled);
    }

    /** Injection point, after a menu row is bound. Rows are reused, so a shown row is reset too. */
    public static void bind(View row, Enum<?> icon) {
        BooleanSupplier hidden = HIDDEN.get(icon.name());
        boolean hide = hidden != null && hidden.getAsBoolean();
        row.setVisibility(hide ? View.GONE : View.VISIBLE);
        // The menu is a list, which keeps a slot for a gone row unless it has no height.
        ViewGroup.LayoutParams params = row.getLayoutParams();
        if (params == null) return;
        int height = hide ? 0 : ViewGroup.LayoutParams.WRAP_CONTENT;
        if (params.height != height) {
            params.height = height;
            row.setLayoutParams(params);
        }
    }
}
