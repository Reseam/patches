// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtubemusic.layout;

import static app.reseam.youtubemusic.core.MusicSettings.toggle;

import android.content.Context;
import android.util.SparseArray;
import android.view.MenuItem;

import java.util.function.BooleanSupplier;

import app.reseam.youtube.core.YouTubeContext;

/** Keeps toolbar items hidden; the app makes them visible again whenever a page changes. */
public final class ToolbarButtons {
    private static volatile SparseArray<BooleanSupplier> hidden;

    private ToolbarButtons() {}

    /** Injection point, in place of every {@link MenuItem#setVisible} call in the app. */
    public static MenuItem setVisible(MenuItem item, boolean visible) {
        BooleanSupplier hide = hidden().get(item.getItemId());
        return item.setVisible(visible && (hide == null || !hide.getAsBoolean()));
    }

    private static SparseArray<BooleanSupplier> hidden() {
        SparseArray<BooleanSupplier> items = hidden;
        if (items != null) return items;
        Context context = YouTubeContext.get();
        items = new SparseArray<>();
        put(context, items, "action_search", "hide_search_button");
        put(context, items, "history_menu_item", "hide_history_button");
        return hidden = items;
    }

    private static void put(Context context, SparseArray<BooleanSupplier> items, String id, String setting) {
        items.put(context.getResources().getIdentifier(id, "id", context.getPackageName()), toggle(setting, false));
    }
}
