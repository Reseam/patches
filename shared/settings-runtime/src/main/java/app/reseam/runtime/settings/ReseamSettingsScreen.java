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
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.function.Consumer;

/**
 * Settings screen built from the bundle's settings schema with plain Android widgets. A host can
 * swap in native-looking toggle rows through {@link #setToggleRowFactory}.
 */
public final class ReseamSettingsScreen {
    public interface ToggleRow {
        View view();

        void setChecked(boolean checked);
    }

    /** Builds one toggle row; the listener must fire whenever the user changes the value. */
    public interface ToggleRowFactory {
        ToggleRow create(Context ctx, String title, String summary, boolean checked, CompoundButton.OnCheckedChangeListener listener);
    }

    private static final String TAG = "ReseamSettings";
    public static final int FOLDER_PICKER_REQUEST_CODE = 0x57C4;
    private static final String PAGE_EXTRA = "app.reseam.settings.PAGE";
    private static final String FOLDER_KEY_EXTRA = "app.reseam.settings.FOLDER_KEY";
    static final int SECONDARY_TEXT = Color.parseColor("#A8A8A8");
    static final int TERTIARY_TEXT = Color.parseColor("#666666");
    static final int FIELD_BACKGROUND = Color.parseColor("#1C1C1C");

    private ReseamSettingsScreen() {}

    public static void setToggleRowFactory(ToggleRowFactory factory) {
        SettingRows.toggleRows = factory;
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

        LinearLayout body = new LinearLayout(ctx);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(0, 0, 0, dpToPx(ctx, 24));
        scroll.addView(body);

        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        body.addView(root);

        container.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        SettingRows rows = new SettingRows(ctx);
        rows.follow(container);

        String title = "Reseam Settings";
        try {
            SettingsSchema schema = SettingsSchema.load(ctx);
            Activity activity = findActivity(ctx);
            String pageId = activity == null ? null : activity.getIntent().getStringExtra(PAGE_EXTRA);
            if (pageId == null) pageId = "";
            if (!pageId.isEmpty()) title = schema.page(pageId).title;
            for (SettingsSchema.Page page : schema.pages) {
                if (pageId.equals(page.parent)) addPage(ctx, root, page);
            }
            for (SettingsSchema.Section section : schema.sections) {
                if (!pageId.equals(section.page) || section.settings.isEmpty()) continue;
                root.addView(sectionHeader(ctx, section.title));
                for (SettingsSchema.Setting setting : section.settings) {
                    root.addView(rows.create(setting));
                }
            }
            if (root.getChildCount() == 0) {
                root.addView(description(ctx, "No settings are available for the selected patches."));
            }
            if (activity != null && pageId.isEmpty()) {
                RestartPrompt.watch(activity);
                SearchResults results = new SearchResults(ctx, schema, rows, root);
                body.addView(results.view());
                container.addView(buildSearchField(ctx, results, scroll), 0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load settings", e);
            body.removeAllViews();
            body.addView(description(ctx, "Could not load settings: " + e.getMessage()));
        }
        container.addView(buildToolbar(ctx, title), 0,
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(ctx, 56)));

        return container;
    }

    private static void addPage(Context ctx, ViewGroup parent, SettingsSchema.Page page) {
        Activity activity = findActivity(ctx);
        if (activity == null) throw new IllegalArgumentException("Settings pages require an Activity context");
        String id = page.id;
        String title = page.title;

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

    private static View buildSearchField(Context ctx, SearchResults results, ScrollView scroll) {
        FrameLayout bar = new FrameLayout(ctx);
        bar.setPadding(dpToPx(ctx, 16), dpToPx(ctx, 4), dpToPx(ctx, 16), dpToPx(ctx, 8));

        EditText field = new EditText(ctx);
        // Stable across activity recreation so Android restores the query, which re-runs the search.
        field.setId(android.R.id.edit);
        field.setHint("Search settings");
        field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        field.setSingleLine(true);
        field.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        field.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
        field.setTextColor(Color.WHITE);
        field.setHintTextColor(TERTIARY_TEXT);
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(FIELD_BACKGROUND);
        shape.setCornerRadius(dpToPx(ctx, 28));
        field.setBackground(shape);
        field.setPadding(dpToPx(ctx, 20), 0, dpToPx(ctx, 48), 0);
        bar.addView(field, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(ctx, 48)));

        TextView clear = text(ctx, "✕", 16f, SECONDARY_TEXT);
        clear.setGravity(Gravity.CENTER);
        clear.setContentDescription("Clear search");
        clear.setVisibility(View.GONE);
        clear.setOnClickListener(v -> field.setText(""));
        bar.addView(clear, new FrameLayout.LayoutParams(dpToPx(ctx, 48), dpToPx(ctx, 48), Gravity.END | Gravity.CENTER_VERTICAL));

        field.addTextChangedListener(afterTextChanged(query -> {
            clear.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
            results.show(query);
            scroll.scrollTo(0, 0);
        }));
        field.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_SEARCH) return false;
            ctx.getSystemService(InputMethodManager.class).hideSoftInputFromWindow(v.getWindowToken(), 0);
            return true;
        });
        return bar;
    }

    /** Reseam's pages are always dark, so its dialogs use the platform's dark theme rather than the host app's. */
    static AlertDialog.Builder dialog(Context ctx) {
        return new AlertDialog.Builder(ctx, android.R.style.Theme_DeviceDefault_Dialog_Alert);
    }

    static void launchFolderPicker(Context ctx, String key) {
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
        return true;
    }

    private static Activity findActivity(Context ctx) {
        while (ctx instanceof android.content.ContextWrapper) {
            if (ctx instanceof Activity) return (Activity) ctx;
            ctx = ((android.content.ContextWrapper) ctx).getBaseContext();
        }
        return null;
    }

    static TextView text(Context ctx, CharSequence value, float sp, int color) {
        TextView tv = new TextView(ctx, null, 0);
        tv.setText(value);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        tv.setTextColor(color);
        return tv;
    }

    static TextView sectionHeader(Context ctx, String title) {
        TextView tv = text(ctx, title, 13f, SECONDARY_TEXT);
        tv.setPadding(dpToPx(ctx, 16), dpToPx(ctx, 16), dpToPx(ctx, 16), dpToPx(ctx, 8));
        return tv;
    }

    static TextView description(Context ctx, String value) {
        TextView tv = text(ctx, value, 13f, SECONDARY_TEXT);
        tv.setPadding(dpToPx(ctx, 16), dpToPx(ctx, 8), dpToPx(ctx, 16), dpToPx(ctx, 8));
        return tv;
    }

    static TextWatcher afterTextChanged(Consumer<String> action) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                action.accept(s.toString());
            }
        };
    }

    static int dpToPx(Context ctx, int dp) {
        float density = ctx.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
