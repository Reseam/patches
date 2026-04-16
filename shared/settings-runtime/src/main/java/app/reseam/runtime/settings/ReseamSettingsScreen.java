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
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Settings screen using Instagram's native IGDS components for a native feel.
 *
 * Uses only non-obfuscated class/method names to remain version-resilient:
 * - Class: com.instagram.igds.components.textcell.IgdsListCell
 * - Methods: getTitleView(), getSubtitleView(), setTextCellType(), setChecked()
 * - Enum values by string name: "TYPE_SWITCH", "TYPE_CHECKBOX", etc.
 */
public final class ReseamSettingsScreen {
    private static final String TAG = "ReseamSettings";
    public static final int FOLDER_PICKER_REQUEST_CODE = 0x57C4;
    private static String pendingFolderKey;

    // Instagram component class name (non-obfuscated, stable across versions)
    private static final String IGDS_LIST_CELL_CLASS = "com.instagram.igds.components.textcell.IgdsListCell";

    // Cached reflection data (initialized once)
    private static boolean reflectionInitialized = false;
    private static Class<?> igdsListCellClass;
    private static Class<?> textCellTypeEnum;
    private static Object typeSwitchValue;
    private static Method setTextCellTypeMethod;
    private static Method setCheckedMethod;
    private static Method getTitleViewMethod;
    private static Method getSubtitleViewMethod;
    private static Method setOnCheckedChangeListenerMethod;

    private ReseamSettingsScreen() {}

    public static View build(Context ctx) {
        ReseamSettings.init(ctx);
        initReflection();

        LinearLayout container = new LinearLayout(ctx);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setBackgroundColor(Color.BLACK);

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

    /**
     * Initialize reflection for Instagram IGDS components.
     * Finds classes and methods by their non-obfuscated names and signatures.
     */
    private static synchronized void initReflection() {
        if (reflectionInitialized) return;
        reflectionInitialized = true;

        try {
            // Load the IgdsListCell class (non-obfuscated name)
            igdsListCellClass = Class.forName(IGDS_LIST_CELL_CLASS);

            // Find setTextCellType method (non-obfuscated name)
            // Its parameter type is the TextCellType enum (obfuscated class name)
            for (Method m : igdsListCellClass.getDeclaredMethods()) {
                if ("setTextCellType".equals(m.getName()) && m.getParameterCount() == 1) {
                    Class<?> paramType = m.getParameterTypes()[0];
                    if (paramType.isEnum()) {
                        setTextCellTypeMethod = m;
                        textCellTypeEnum = paramType;
                        break;
                    }
                }
            }

            if (textCellTypeEnum == null) {
                throw new RuntimeException("Could not find TextCellType enum via setTextCellType method");
            }

            // Find TYPE_SWITCH enum value by its non-obfuscated string name
            typeSwitchValue = findEnumByName(textCellTypeEnum, "TYPE_SWITCH");
            if (typeSwitchValue == null) {
                throw new RuntimeException("Could not find TYPE_SWITCH enum value");
            }

            // Find non-obfuscated methods
            setCheckedMethod = igdsListCellClass.getMethod("setChecked", boolean.class);
            getTitleViewMethod = igdsListCellClass.getMethod("getTitleView");
            getSubtitleViewMethod = igdsListCellClass.getMethod("getSubtitleView");

            // Find the listener setter by parameter type (method name is obfuscated)
            for (Method m : igdsListCellClass.getDeclaredMethods()) {
                Class<?>[] params = m.getParameterTypes();
                if (params.length == 1 && CompoundButton.OnCheckedChangeListener.class.isAssignableFrom(params[0])) {
                    setOnCheckedChangeListenerMethod = m;
                    setOnCheckedChangeListenerMethod.setAccessible(true);
                    break;
                }
            }

            Log.i(TAG, "IGDS components initialized successfully");

        } catch (Throwable t) {
            Log.e(TAG, "Failed to initialize IGDS reflection", t);
            throw new RuntimeException("IGDS components required but not available", t);
        }
    }

    /**
     * Find an enum constant by its name (non-obfuscated).
     */
    @SuppressWarnings("unchecked")
    private static Object findEnumByName(Class<?> enumClass, String name) {
        try {
            return Enum.valueOf((Class<Enum>) enumClass, name);
        } catch (Throwable t) {
            for (Object constant : enumClass.getEnumConstants()) {
                if (name.equals(((Enum<?>) constant).name())) {
                    return constant;
                }
            }
            return null;
        }
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

    /**
     * Create a toggle row using Instagram's native IgdsListCell component.
     */
    private static void addToggle(Context ctx, ViewGroup parent, String title, String summary, String key, boolean defaultValue) {
        try {
            // Create IgdsListCell using its Context constructor
            View cell = (View) igdsListCellClass.getConstructor(Context.class).newInstance(ctx);

            // Set the cell type to TYPE_SWITCH
            setTextCellTypeMethod.invoke(cell, typeSwitchValue);

            // Set title using getTitleView().setText()
            TextView titleView = (TextView) getTitleViewMethod.invoke(cell);
            if (titleView != null) {
                titleView.setText(title);
            }

            // Set subtitle if present
            if (summary != null && !summary.isEmpty()) {
                TextView subtitleView = (TextView) getSubtitleViewMethod.invoke(cell);
                if (subtitleView != null) {
                    subtitleView.setText(summary);
                    subtitleView.setVisibility(View.VISIBLE);
                }
            }

            // Set initial checked state
            boolean currentValue = ReseamSettings.getBoolean(key, defaultValue);
            setCheckedMethod.invoke(cell, currentValue);

            // Set the listener
            if (setOnCheckedChangeListenerMethod != null) {
                CompoundButton.OnCheckedChangeListener listener = (buttonView, isChecked) -> {
                    ReseamSettings.setBoolean(key, isChecked);
                };
                setOnCheckedChangeListenerMethod.invoke(cell, listener);
            }

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            parent.addView(cell, lp);

        } catch (Throwable t) {
            Log.e(TAG, "Failed to create IGDS toggle for: " + title, t);
            throw new RuntimeException("Failed to create toggle: " + title, t);
        }
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
