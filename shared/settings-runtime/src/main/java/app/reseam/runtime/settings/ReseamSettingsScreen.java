// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later


package app.reseam.runtime.settings;

import android.app.Activity;
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
    private static String pendingFolderKey;

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

        container.addView(buildToolbar(ctx, "Reseam Settings"),
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(ctx, 56)));

        ScrollView scroll = new ScrollView(ctx);
        scroll.setBackgroundColor(Color.BLACK);

        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(0, 0, 0, dpToPx(ctx, 24));
        scroll.addView(root);

        container.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        try {
            JSONObject schema = new JSONObject(readAsset(ctx, "reseam/settings.json"));
            JSONArray sections = schema.optJSONArray("sections");
            if (sections == null || sections.length() == 0) {
                addDescription(root, "No settings are available for the selected patches.");
                return container;
            }
            for (int i = 0; i < sections.length(); i++) {
                JSONObject section = sections.getJSONObject(i);
                addSectionHeader(root, section.optString("title", "Settings"));
                JSONArray settings = section.optJSONArray("settings");
                if (settings == null) continue;
                for (int j = 0; j < settings.length(); j++) {
                    addSetting(ctx, root, settings.getJSONObject(j));
                }
            }
        } catch (Throwable t) {
            Log.e(TAG, "Failed to load settings", t);
            addDescription(root, "Could not load settings: " + t.getMessage());
        }

        return container;
    }

    private static View buildToolbar(Context ctx, String title) {
        FrameLayout bar = new FrameLayout(ctx);
        bar.setBackgroundColor(Color.BLACK);
        bar.setPadding(dpToPx(ctx, 4), 0, dpToPx(ctx, 16), 0);

        ImageView back = new ImageView(ctx);
        back.setImageResource(android.R.drawable.ic_media_previous);
        int backRes = ctx.getResources().getIdentifier("ic_arrow_back", "drawable", "android");
        if (backRes != 0) back.setImageResource(backRes);
        back.setColorFilter(Color.WHITE);
        int pad = dpToPx(ctx, 12);
        back.setPadding(pad, pad, pad, pad);
        back.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        back.setOnClickListener(v -> {
            if (ctx instanceof Activity) ((Activity) ctx).finish();
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
        } else if ("text".equals(type) || "choice".equals(type)) {
            addTextSetting(parent, title, summary, key, setting.optString("default", ""));
        }
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
        pendingFolderKey = key;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        activity.startActivityForResult(intent, FOLDER_PICKER_REQUEST_CODE);
    }

    public static boolean onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        if (requestCode != FOLDER_PICKER_REQUEST_CODE) return false;
        String key = pendingFolderKey;
        pendingFolderKey = null;
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

        View divider = new View(ctx);
        divider.setBackgroundColor(Color.parseColor("#1C1C1C"));
        parent.addView(divider, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(ctx, 8)));

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
