// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.runtime.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the row for each setting and keeps every row in step with its stored value, so a setting
 * shown on a page and in search results agrees after a change made in either place.
 */
final class SettingRows implements SharedPreferences.OnSharedPreferenceChangeListener {
    static volatile ReseamSettingsScreen.ToggleRowFactory toggleRows = SettingRows::plainToggleRow;

    private final Context ctx;
    private final Map<String, List<Runnable>> refreshes = new HashMap<>();

    SettingRows(Context ctx) {
        this.ctx = ctx;
    }

    /** Preferences hold their listeners weakly, so the screen's lifetime keeps this one registered. */
    void follow(View screen) {
        screen.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                ReseamSettings.prefs().registerOnSharedPreferenceChangeListener(SettingRows.this);
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                ReseamSettings.prefs().unregisterOnSharedPreferenceChangeListener(SettingRows.this);
            }
        });
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        List<Runnable> rows = refreshes.get(key);
        if (rows == null) return;
        for (Runnable refresh : rows) refresh.run();
    }

    View create(SettingsSchema.Setting setting) {
        if (setting instanceof SettingsSchema.Toggle) return toggle((SettingsSchema.Toggle) setting);
        if (setting instanceof SettingsSchema.Choice) return choice((SettingsSchema.Choice) setting);
        if (setting instanceof SettingsSchema.Folder) return folder((SettingsSchema.Folder) setting);
        if (setting instanceof SettingsSchema.Text) return text((SettingsSchema.Text) setting);
        throw new IllegalArgumentException("Unknown setting type: " + setting.getClass().getName());
    }

    private void onChange(String key, Runnable refresh) {
        refreshes.computeIfAbsent(key, k -> new ArrayList<>()).add(refresh);
    }

    private View toggle(SettingsSchema.Toggle toggle) {
        String key = toggle.key;
        ReseamSettingsScreen.ToggleRow row = toggleRows.create(ctx, toggle.title, toggle.summary,
                ReseamSettings.getBoolean(key, toggle.defaultValue), (button, isChecked) -> ReseamSettings.setBoolean(key, isChecked));
        onChange(key, () -> row.setChecked(ReseamSettings.getBoolean(key, toggle.defaultValue)));
        View view = row.view();
        view.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return view;
    }

    private View choice(SettingsSchema.Choice choice) {
        TextView value = valueText();
        Runnable refresh = () -> value.setText(choice.titles[selectedIndex(choice)]);
        refresh.run();
        onChange(choice.key, refresh);
        return valueRow(choice, value, v -> ReseamSettingsScreen.dialog(ctx)
                .setTitle(choice.title)
                .setSingleChoiceItems(choice.titles, selectedIndex(choice), (dialog, which) -> {
                    ReseamSettings.setString(choice.key, choice.values[which]);
                    dialog.dismiss();
                })
                .show());
    }

    private static int selectedIndex(SettingsSchema.Choice choice) {
        return choice.index(ReseamSettings.getString(choice.key, choice.defaultValue));
    }

    private View folder(SettingsSchema.Folder folder) {
        TextView value = valueText();
        Runnable refresh = () -> value.setText(displayFolder(ReseamSettings.getString(folder.key, folder.defaultValue)));
        refresh.run();
        onChange(folder.key, refresh);
        return valueRow(folder, value, v -> ReseamSettingsScreen.launchFolderPicker(ctx, folder.key));
    }

    private static String displayFolder(String value) {
        if (value == null || value.isEmpty()) return "(not set)";
        if (value.startsWith("content://")) {
            try {
                Uri uri = Uri.parse(value);
                String last = uri.getLastPathSegment();
                if (last != null) {
                    int colon = last.lastIndexOf(':');
                    if (colon >= 0 && colon + 1 < last.length()) last = last.substring(colon + 1);
                    return last.isEmpty() ? value : last;
                }
            } catch (Throwable ignored) {}
        }
        return value;
    }

    private TextView valueText() {
        TextView value = ReseamSettingsScreen.text(ctx, "", 13f, ReseamSettingsScreen.SECONDARY_TEXT);
        value.setPadding(0, ReseamSettingsScreen.dpToPx(ctx, 4), 0, 0);
        return value;
    }

    private View valueRow(SettingsSchema.Setting setting, TextView value, View.OnClickListener onClick) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(ReseamSettingsScreen.dpToPx(ctx, 16), ReseamSettingsScreen.dpToPx(ctx, 12),
                ReseamSettingsScreen.dpToPx(ctx, 16), ReseamSettingsScreen.dpToPx(ctx, 12));
        row.setClickable(true);
        row.setFocusable(true);
        row.addView(ReseamSettingsScreen.text(ctx, setting.title, 16f, Color.WHITE));
        row.addView(value);
        if (setting.summary != null && !setting.summary.isEmpty()) {
            TextView sub = ReseamSettingsScreen.text(ctx, setting.summary, 12f, ReseamSettingsScreen.TERTIARY_TEXT);
            sub.setPadding(0, ReseamSettingsScreen.dpToPx(ctx, 2), 0, 0);
            row.addView(sub);
        }
        row.setOnClickListener(onClick);
        return row;
    }

    private View text(SettingsSchema.Text text) {
        String key = text.key;

        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(ReseamSettingsScreen.dpToPx(ctx, 16), ReseamSettingsScreen.dpToPx(ctx, 12),
                ReseamSettingsScreen.dpToPx(ctx, 16), ReseamSettingsScreen.dpToPx(ctx, 12));
        row.addView(ReseamSettingsScreen.text(ctx, text.title, 16f, Color.WHITE));

        if (text.summary != null && !text.summary.isEmpty()) {
            TextView sub = ReseamSettingsScreen.text(ctx, text.summary, 13f, ReseamSettingsScreen.SECONDARY_TEXT);
            sub.setPadding(0, ReseamSettingsScreen.dpToPx(ctx, 2), 0, ReseamSettingsScreen.dpToPx(ctx, 6));
            row.addView(sub);
        }

        // The default EditText style is what makes the field focusable by touch.
        EditText edit = new EditText(ctx);
        // Values are identifiers, URLs, colors and numbers, never prose.
        edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                | (text.multiline ? InputType.TYPE_TEXT_FLAG_MULTI_LINE : 0));
        if (text.multiline) {
            edit.setMinLines(3);
            edit.setGravity(Gravity.TOP | Gravity.START);
        } else {
            edit.setSingleLine(true);
        }
        edit.setTextColor(Color.WHITE);
        edit.setHintTextColor(ReseamSettingsScreen.TERTIARY_TEXT);
        edit.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
        edit.setBackgroundColor(ReseamSettingsScreen.FIELD_BACKGROUND);
        edit.setPadding(ReseamSettingsScreen.dpToPx(ctx, 12), ReseamSettingsScreen.dpToPx(ctx, 10),
                ReseamSettingsScreen.dpToPx(ctx, 12), ReseamSettingsScreen.dpToPx(ctx, 10));
        Runnable refresh = () -> {
            String stored = ReseamSettings.getString(key, text.defaultValue);
            if (!stored.equals(edit.getText().toString())) edit.setText(stored);
        };
        // Filled before the watcher is attached, which would otherwise store the default.
        refresh.run();
        onChange(key, refresh);
        // Leaving the screen does not clear focus, so persist every edit.
        edit.addTextChangedListener(ReseamSettingsScreen.afterTextChanged(value -> ReseamSettings.setString(key, value)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = ReseamSettingsScreen.dpToPx(ctx, 4);
        row.addView(edit, lp);
        return row;
    }

    private static ReseamSettingsScreen.ToggleRow plainToggleRow(Context ctx, String title, String summary, boolean checked,
                                                                 CompoundButton.OnCheckedChangeListener listener) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(ReseamSettingsScreen.dpToPx(ctx, 16), ReseamSettingsScreen.dpToPx(ctx, 12),
                ReseamSettingsScreen.dpToPx(ctx, 16), ReseamSettingsScreen.dpToPx(ctx, 12));

        LinearLayout text = new LinearLayout(ctx);
        text.setOrientation(LinearLayout.VERTICAL);
        text.addView(ReseamSettingsScreen.text(ctx, title, 16f, Color.WHITE));
        if (summary != null && !summary.isEmpty()) {
            TextView sub = ReseamSettingsScreen.text(ctx, summary, 13f, ReseamSettingsScreen.SECONDARY_TEXT);
            sub.setPadding(0, ReseamSettingsScreen.dpToPx(ctx, 2), 0, 0);
            text.addView(sub);
        }
        row.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Switch toggle = new Switch(ctx);
        toggle.setChecked(checked);
        toggle.setOnCheckedChangeListener(listener);
        LinearLayout.LayoutParams toggleLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        toggleLp.leftMargin = ReseamSettingsScreen.dpToPx(ctx, 16);
        row.addView(toggle, toggleLp);

        row.setOnClickListener(v -> toggle.toggle());
        return new ReseamSettingsScreen.ToggleRow() {
            @Override
            public View view() {
                return row;
            }

            @Override
            public void setChecked(boolean checked) {
                toggle.setChecked(checked);
            }
        };
    }
}
