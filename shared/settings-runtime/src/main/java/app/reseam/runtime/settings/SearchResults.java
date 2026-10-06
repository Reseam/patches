// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.runtime.settings;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** The settings of every page that match a query, shown in place of the root page's own rows. */
final class SearchResults {
    private final Context ctx;
    private final SettingsSchema schema;
    private final SettingRows rows;
    private final View page;
    private final LinearLayout view;
    private final View empty;
    private final Map<SettingsSchema.Section, View> headers = new HashMap<>();
    private final Map<String, View> settings = new HashMap<>();

    SearchResults(Context ctx, SettingsSchema schema, SettingRows rows, View page) {
        this.ctx = ctx;
        this.schema = schema;
        this.rows = rows;
        this.page = page;
        view = new LinearLayout(ctx);
        view.setOrientation(LinearLayout.VERTICAL);
        view.setVisibility(View.GONE);
        empty = ReseamSettingsScreen.description(ctx, "No settings match your search.");
    }

    View view() {
        return view;
    }

    void show(String query) {
        String[] words = words(query);
        boolean searching = words.length > 0;
        page.setVisibility(searching ? View.GONE : View.VISIBLE);
        view.setVisibility(searching ? View.VISIBLE : View.GONE);
        view.removeAllViews();
        if (!searching) return;
        for (SettingsSchema.Section section : schema.sections) {
            boolean headed = false;
            for (SettingsSchema.Setting setting : section.settings) {
                if (!matches(setting, words)) continue;
                if (!headed) {
                    view.addView(headers.computeIfAbsent(section, this::header));
                    headed = true;
                }
                view.addView(settings.computeIfAbsent(setting.key, key -> rows.create(setting)));
            }
        }
        if (view.getChildCount() == 0) view.addView(empty);
    }

    private View header(SettingsSchema.Section section) {
        String path = schema.path(section.page);
        return ReseamSettingsScreen.sectionHeader(ctx, path.isEmpty() ? section.title : path + " › " + section.title);
    }

    private static String[] words(String query) {
        String normalized = query.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? new String[0] : normalized.split("\\s+");
    }

    private static boolean matches(SettingsSchema.Setting setting, String[] words) {
        String text = (setting.summary == null ? setting.title : setting.title + "\n" + setting.summary).toLowerCase(Locale.ROOT);
        for (String word : words) {
            if (!text.contains(word)) return false;
        }
        return true;
    }
}
