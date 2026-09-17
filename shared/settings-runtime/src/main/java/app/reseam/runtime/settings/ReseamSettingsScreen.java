// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later


package app.reseam.runtime.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * Settings screen built from the bundle's settings schema with plain Android widgets. A host can
 * swap in native-looking toggle rows through {@link #setToggleRowFactory}.
 */
public final class ReseamSettingsScreen {
    /** Builds one toggle row; the listener must fire whenever the user changes the value. */
    public interface ToggleRowFactory {
        View create(Context ctx, String title, String summary, boolean checked, CompoundButton.OnCheckedChangeListener listener);
    }

    private static final String TAG = "ReseamSettings";
    public static final int FOLDER_PICKER_REQUEST_CODE = 0x57C4;
    private static final String PAGE_EXTRA = "app.reseam.settings.PAGE";
    private static final String FOLDER_KEY_EXTRA = "app.reseam.settings.FOLDER_KEY";

    private static volatile ToggleRowFactory toggleRows = ReseamSettingsScreen::plainToggleRow;

    private ReseamSettingsScreen() {}

    public static void setToggleRowFactory(ToggleRowFactory factory) {
        toggleRows = factory;
    }

    public static View build(Context ctx) {
        ReseamSettings.init(ctx);

        LinearLayout container = new LinearLayout(ctx);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setBackgroundColor(Color.BLACK);
        // Hosts targeting API 35+ draw edge to edge; keep the toolbar below the status bar.
        container.setFitsSystemWindows(true);

        ScrollView scroll = new ScrollView(ctx);
        // Stable across activity recreation so Android restores each page's scroll position.
        scroll.setId(android.R.id.list);
        scroll.setBackgroundColor(Color.BLACK);

        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(0, 0, 0, dpToPx(ctx, 24));
        scroll.addView(root);

        container.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        String title = "Reseam Settings";
        try {
            JSONObject schema = new JSONObject(readAsset(ctx, "reseam/settings.json"));
            Activity activity = findActivity(ctx);
            String pageId = activity == null ? null : activity.getIntent().getStringExtra(PAGE_EXTRA);
            if (pageId == null) pageId = "";
            JSONArray pages = schema.optJSONArray("pages");
            boolean foundPage = pageId.isEmpty();
            if (pages != null) {
                for (int i = 0; i < pages.length(); i++) {
                    JSONObject page = pages.getJSONObject(i);
                    if (pageId.equals(page.getString("id"))) {
                        title = page.getString("title");
                        foundPage = true;
                    }
                    if (pageId.equals(destination(page, "parent"))) {
                        addPage(ctx, root, page);
                    }
                }
            }
            if (!foundPage) throw new IllegalArgumentException("Unknown settings page: " + pageId);
            JSONArray sections = schema.optJSONArray("sections");
            if (sections != null) {
                for (int i = 0; i < sections.length(); i++) {
                    JSONObject section = sections.getJSONObject(i);
                    if (!pageId.equals(destination(section, "page"))) continue;
                    JSONArray settings = section.optJSONArray("settings");
                    if (settings == null || settings.length() == 0) continue;
                    addSectionHeader(root, section.getString("title"));
                    for (int j = 0; j < settings.length(); j++) {
                        addSetting(ctx, root, settings.getJSONObject(j));
                    }
                }
            }
            if (root.getChildCount() == 0) {
                addDescription(root, "No settings are available for the selected patches.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load settings", e);
            root.removeAllViews();
            addDescription(root, "Could not load settings: " + e.getMessage());
        }
        container.addView(buildToolbar(ctx, title), 0,
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(ctx, 56)));

        return container;
    }

    private static String destination(JSONObject object, String key) {
        return object.isNull(key) ? "" : object.optString(key, "");
    }

    private static void addPage(Context ctx, ViewGroup parent, JSONObject page) throws org.json.JSONException {
        Activity activity = findActivity(ctx);
        if (activity == null) throw new IllegalArgumentException("Settings pages require an Activity context");
        String id = page.getString("id");
        String title = page.getString("title");

        LinearLayout row = new LinearLayout(ctx);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dpToPx(ctx, 16), dpToPx(ctx, 18), dpToPx(ctx, 16), dpToPx(ctx, 18));
        row.setFocusable(true);
        TypedValue background = new TypedValue();
        if (ctx.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, background, true)) {
            row.setBackgroundResource(background.resourceId);
        }

        TextView label = new TextView(ctx);
        label.setText(title);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
        label.setTextColor(Color.WHITE);
        row.addView(label, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView arrow = new TextView(ctx);
        arrow.setText("›");
        arrow.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f);
        arrow.setTextColor(Color.LTGRAY);
        arrow.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        row.addView(arrow);
        row.setOnClickListener(v -> {
            // Reuse the host's activity and theme. Android owns the page stack and Back gestures.
            Intent intent = new Intent(activity, activity.getClass());
            intent.putExtra(PAGE_EXTRA, id);
            activity.startActivity(intent);
        });
        parent.addView(row);
    }

    private static View buildToolbar(Context ctx, String title) {
        FrameLayout bar = new FrameLayout(ctx);
        bar.setBackgroundColor(Color.BLACK);
        bar.setPadding(dpToPx(ctx, 4), 0, dpToPx(ctx, 16), 0);

        ImageView back = new ImageView(ctx);
        back.setImageDrawable(new BackArrowDrawable(dpToPx(ctx, 24)));
        int pad = dpToPx(ctx, 12);
        back.setPadding(pad, pad, pad, pad);
        back.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        back.setContentDescription("Back");
        back.setOnClickListener(v -> {
            Activity activity = findActivity(ctx);
            if (activity != null) activity.finish();
        });
        FrameLayout.LayoutParams backLp = new FrameLayout.LayoutParams(
                dpToPx(ctx, 48), dpToPx(ctx, 48), Gravity.START | Gravity.CENTER_VERTICAL);
        bar.addView(back, backLp);

        TextView tv = new TextView(ctx);
        tv.setText(title);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setTextColor(Color.WHITE);
        FrameLayout.LayoutParams tvLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.START | Gravity.CENTER_VERTICAL);
        tvLp.leftMargin = dpToPx(ctx, 56);
        bar.addView(tv, tvLp);

        return bar;
    }

    private static void addSetting(Context ctx, ViewGroup parent, JSONObject setting) {
        String type = setting.optString("type", "");
        String key = setting.optString("key", "");
        String title = setting.optString("title", key);
        String summary = setting.isNull("summary") ? null : setting.optString("summary", null);

        if ("toggle".equals(type)) {
            addToggle(ctx, parent, title, summary, key, setting.optBoolean("default", false));
        } else if ("folder".equals(type)) {
            addFolderPicker(ctx, parent, title, summary, key, setting.optString("default", ""));
        } else if ("choice".equals(type)) {
            addChoice(ctx, parent, title, summary, key, setting.optString("default", ""), setting.optJSONArray("choices"));
        } else if ("text".equals(type)) {
            addTextSetting(parent, title, summary, key, setting.optString("default", ""));
        }
    }

    private static void addChoice(Context ctx, ViewGroup parent, String title, String summary, String key,
                                  String defaultValue, JSONArray choices) {
        if (choices == null || choices.length() == 0) {
            addTextSetting(parent, title, summary, key, defaultValue);
            return;
        }

        String[] values = new String[choices.length()];
        String[] labels = new String[choices.length()];
        for (int i = 0; i < choices.length(); i++) {
            JSONObject choice = choices.optJSONObject(i);
            values[i] = choice == null ? "" : choice.optString("value", "");
            labels[i] = choice == null ? "" : choice.optString("title", values[i]);
        }

        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dpToPx(ctx, 16), dpToPx(ctx, 12), dpToPx(ctx, 16), dpToPx(ctx, 12));
        row.setClickable(true);
        row.setFocusable(true);

        TextView titleView = new TextView(ctx, null, 0);
        titleView.setText(title);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
        titleView.setTextColor(Color.WHITE);
        row.addView(titleView);

        TextView valueView = new TextView(ctx, null, 0);
        valueView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        valueView.setTextColor(Color.parseColor("#A8A8A8"));
        valueView.setPadding(0, dpToPx(ctx, 4), 0, 0);
        valueView.setText(labels[selectedIndex(values, ReseamSettings.getString(key, defaultValue))]);
        row.addView(valueView);

        if (summary != null && !summary.isEmpty()) {
            TextView sub = new TextView(ctx, null, 0);
            sub.setText(summary);
            sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
            sub.setTextColor(Color.parseColor("#666666"));
            sub.setPadding(0, dpToPx(ctx, 2), 0, 0);
            row.addView(sub);
        }

        row.setOnClickListener(v -> new AlertDialog.Builder(ctx)
                .setTitle(title)
                .setSingleChoiceItems(labels, selectedIndex(values, ReseamSettings.getString(key, defaultValue)), (dialog, which) -> {
                    ReseamSettings.setString(key, values[which]);
                    valueView.setText(labels[which]);
                    dialog.dismiss();
                })
                .show());
        parent.addView(row);
    }

    /** Falls back to the first choice, so the row always shows something the list can highlight. */
    private static int selectedIndex(String[] values, String current) {
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(current)) return i;
        }
        return 0;
    }

    private static void addFolderPicker(Context ctx, ViewGroup parent, String title, String summary, String key, String defaultValue) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dpToPx(ctx, 16), dpToPx(ctx, 12), dpToPx(ctx, 16), dpToPx(ctx, 12));
        row.setClickable(true);
        row.setFocusable(true);

        TextView titleView = new TextView(ctx, null, 0);
        titleView.setText(title);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
        titleView.setTextColor(Color.WHITE);
        row.addView(titleView);

        TextView valueView = new TextView(ctx, null, 0);
        valueView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        valueView.setTextColor(Color.parseColor("#A8A8A8"));
        valueView.setPadding(0, dpToPx(ctx, 4), 0, 0);
        valueView.setText(displayFolder(ReseamSettings.getString(key, defaultValue)));
        row.addView(valueView);

        if (summary != null && !summary.isEmpty()) {
            TextView sub = new TextView(ctx, null, 0);
            sub.setText(summary);
            sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
            sub.setTextColor(Color.parseColor("#666666"));
            sub.setPadding(0, dpToPx(ctx, 2), 0, 0);
            row.addView(sub);
        }

        row.setOnClickListener(v -> launchFolderPicker(ctx, key));
        parent.addView(row);
    }

    private static void launchFolderPicker(Context ctx, String key) {
        Activity activity = findActivity(ctx);
        if (activity == null) {
            Log.e(TAG, "Cannot launch folder picker: no Activity context");
            return;
        }
        activity.getIntent().putExtra(FOLDER_KEY_EXTRA, key);
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        activity.startActivityForResult(intent, FOLDER_PICKER_REQUEST_CODE);
    }

    public static boolean onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        if (requestCode != FOLDER_PICKER_REQUEST_CODE) return false;
        String key = activity.getIntent().getStringExtra(FOLDER_KEY_EXTRA);
        activity.getIntent().removeExtra(FOLDER_KEY_EXTRA);
        if (resultCode != Activity.RESULT_OK || data == null || key == null) return true;
        Uri uri = data.getData();
        if (uri == null) return true;
        try {
            int flags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
            ContentResolver resolver = activity.getContentResolver();
            resolver.takePersistableUriPermission(uri, flags);
        } catch (SecurityException e) {
            Log.w(TAG, "Could not persist permission for " + uri, e);
        }
        ReseamSettings.setString(key, uri.toString());
        ScrollView previous = activity.findViewById(android.R.id.list);
        int scrollY = previous == null ? 0 : previous.getScrollY();
        activity.setContentView(build(activity));
        ScrollView current = activity.findViewById(android.R.id.list);
        current.post(() -> current.scrollTo(0, scrollY));
        return true;
    }

    private static Activity findActivity(Context ctx) {
        while (ctx instanceof android.content.ContextWrapper) {
            if (ctx instanceof Activity) return (Activity) ctx;
            ctx = ((android.content.ContextWrapper) ctx).getBaseContext();
        }
        return null;
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

    private static void addToggle(Context ctx, ViewGroup parent, String title, String summary, String key, boolean defaultValue) {
        boolean checked = ReseamSettings.getBoolean(key, defaultValue);
        View row = toggleRows.create(ctx, title, summary, checked, (button, isChecked) -> ReseamSettings.setBoolean(key, isChecked));
        parent.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private static View plainToggleRow(Context ctx, String title, String summary, boolean checked, CompoundButton.OnCheckedChangeListener listener) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dpToPx(ctx, 16), dpToPx(ctx, 12), dpToPx(ctx, 16), dpToPx(ctx, 12));

        LinearLayout text = new LinearLayout(ctx);
        text.setOrientation(LinearLayout.VERTICAL);
        TextView titleView = new TextView(ctx, null, 0);
        titleView.setText(title);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
        titleView.setTextColor(Color.WHITE);
        text.addView(titleView);
        if (summary != null && !summary.isEmpty()) {
            TextView sub = new TextView(ctx, null, 0);
            sub.setText(summary);
            sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
            sub.setTextColor(Color.parseColor("#A8A8A8"));
            sub.setPadding(0, dpToPx(ctx, 2), 0, 0);
            text.addView(sub);
        }
        row.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Switch toggle = new Switch(ctx);
        toggle.setChecked(checked);
        toggle.setOnCheckedChangeListener(listener);
        LinearLayout.LayoutParams toggleLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        toggleLp.leftMargin = dpToPx(ctx, 16);
        row.addView(toggle, toggleLp);

        row.setOnClickListener(v -> toggle.toggle());
        return row;
    }

    private static void addSectionHeader(ViewGroup parent, String title) {
        Context ctx = parent.getContext();

        TextView tv = new TextView(ctx, null, 0);
        tv.setText(title);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        tv.setTextColor(Color.parseColor("#A8A8A8"));
        tv.setPadding(dpToPx(ctx, 16), dpToPx(ctx, 16), dpToPx(ctx, 16), dpToPx(ctx, 8));
        parent.addView(tv);
    }

    private static void addDescription(ViewGroup parent, String text) {
        Context ctx = parent.getContext();
        TextView tv = new TextView(ctx, null, 0);
        tv.setText(text);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        tv.setTextColor(Color.parseColor("#A8A8A8"));
        tv.setPadding(dpToPx(ctx, 16), dpToPx(ctx, 8), dpToPx(ctx, 16), dpToPx(ctx, 8));
        parent.addView(tv);
    }

    private static void addTextSetting(ViewGroup parent, String label, String summary, String key, String defaultValue) {
        Context ctx = parent.getContext();

        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dpToPx(ctx, 16), dpToPx(ctx, 12), dpToPx(ctx, 16), dpToPx(ctx, 12));

        TextView tv = new TextView(ctx, null, 0);
        tv.setText(label);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
        tv.setTextColor(Color.WHITE);
        row.addView(tv);

        if (summary != null && !summary.isEmpty()) {
            TextView sub = new TextView(ctx, null, 0);
            sub.setText(summary);
            sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
            sub.setTextColor(Color.parseColor("#A8A8A8"));
            sub.setPadding(0, dpToPx(ctx, 2), 0, dpToPx(ctx, 6));
            row.addView(sub);
        }

        EditText edit = new EditText(ctx, null, 0);
        edit.setSingleLine(true);
        edit.setText(ReseamSettings.getString(key, defaultValue));
        edit.setTextColor(Color.WHITE);
        edit.setHintTextColor(Color.parseColor("#666666"));
        edit.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
        edit.setBackgroundColor(Color.parseColor("#1C1C1C"));
        edit.setPadding(dpToPx(ctx, 12), dpToPx(ctx, 10), dpToPx(ctx, 12), dpToPx(ctx, 10));
        edit.setOnFocusChangeListener((view, hasFocus) -> {
            if (!hasFocus) {
                ReseamSettings.setString(key, edit.getText().toString());
            }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dpToPx(ctx, 4);
        row.addView(edit, lp);

        parent.addView(row);
    }

    private static int dpToPx(Context ctx, int dp) {
        float density = ctx.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private static String readAsset(Context ctx, String path) throws Exception {
        InputStream in = ctx.getAssets().open(path);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int read;
            while ((read = in.read(buf)) != -1) {
                out.write(buf, 0, read);
            }
            return out.toString("UTF-8");
        } finally {
            in.close();
        }
    }
}
