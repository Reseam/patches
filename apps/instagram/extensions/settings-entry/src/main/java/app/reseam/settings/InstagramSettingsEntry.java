// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later


package app.reseam.instagram.settings;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import app.reseam.runtime.settings.ReseamSettings;

public final class InstagramSettingsEntry {
    private static final String TAG = "ReseamSettings";
    private static final int RESEAM_VIEW_TAG = 0x7f5151cc;
    private static final int RESEAM_DECOR_TAG = 0x7f5151cd;
    private static final int RESEAM_REPOST_VISIBILITY_TAG = 0x7f5151ce;
    private static final String HIDE_REPOST_SETTING = "appearance_settings.hide_repost_buttons";
    private static final String[] REPOST_VIEW_NAMES = {
        "clips_repost_button", "repost_button", "repost_button_container",
        "row_feed_button_repost", "tisu_ufi_repost_button"
    };
    private static boolean hookInstalled = false;
    private static int[] repostViewIds;

    private InstagramSettingsEntry() {}

    public static void init(Context ctx) {
        Log.i(TAG, "InstagramSettingsEntry.init() called");
        ReseamSettings.init(ctx);
        Context app = ctx == null ? null : ctx.getApplicationContext();
        if (app instanceof Application) {
            repostViewIds = resolveRepostViewIds(app);
            installLongPressHook((Application) app);
        }
    }

    private static int[] resolveRepostViewIds(Context context) {
        int[] ids = new int[REPOST_VIEW_NAMES.length];
        for (int i = 0; i < REPOST_VIEW_NAMES.length; i++) {
            ids[i] = context.getResources().getIdentifier(REPOST_VIEW_NAMES[i], "id", context.getPackageName());
        }
        return ids;
    }

    private static void installLongPressHook(Application app) {
        if (hookInstalled) return;
        hookInstalled = true;
        Log.i(TAG, "Installing activity lifecycle hook for long-press detection");
        app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override public void onActivityCreated(Activity a, Bundle b) {}
            @Override public void onActivityStarted(Activity a) {}
            @Override public void onActivityResumed(Activity a) { attachOnceLaidOut(a); }
            @Override public void onActivityPaused(Activity a) {}
            @Override public void onActivityStopped(Activity a) {}
            @Override public void onActivitySaveInstanceState(Activity a, Bundle b) {}
            @Override public void onActivityDestroyed(Activity a) {}
        });
    }

    private static void attachOnceLaidOut(final Activity activity) {
        final View decor = activity.getWindow() == null ? null : activity.getWindow().getDecorView();
        if (decor == null || decor.getTag(RESEAM_DECOR_TAG) != null) return;
        decor.setTag(RESEAM_DECOR_TAG, Boolean.TRUE);
        decor.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override public void onGlobalLayout() {
                walk(decor, activity, ReseamSettings.getBoolean(HIDE_REPOST_SETTING, false));
            }
        });
        walk(decor, activity, ReseamSettings.getBoolean(HIDE_REPOST_SETTING, false));
    }

    private static void walk(View v, Activity activity, boolean hideRepost) {
        if (v == null) return;
        if (isRepostView(v.getId())) {
            Object original = v.getTag(RESEAM_REPOST_VISIBILITY_TAG);
            if (hideRepost) {
                if (original == null) v.setTag(RESEAM_REPOST_VISIBILITY_TAG, v.getVisibility());
                if (v.getVisibility() != View.GONE) v.setVisibility(View.GONE);
            } else if (original instanceof Integer) {
                v.setTag(RESEAM_REPOST_VISIBILITY_TAG, null);
                v.setVisibility((Integer) original);
            }
        }
        CharSequence cd = v.getContentDescription();
        if (cd != null) {
            String s = cd.toString().toLowerCase();
            // Match hamburger/overflow menu buttons
            if (s.contains("menu") || s.contains("options") || s.contains("more") || s.contains("hamburger")) {
                if (v.getTag(RESEAM_VIEW_TAG) == null) {
                    v.setTag(RESEAM_VIEW_TAG, Boolean.TRUE);
                    // Make sure the view can receive long-click events
                    v.setLongClickable(true);
                    v.setOnLongClickListener(view -> {
                        Log.i(TAG, "Long-press detected, opening Reseam Settings");
                        Context c = view.getContext();
                        Intent i = new Intent(c, InstagramReseamSettingsActivity.class);
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        c.startActivity(i);
                        return true;
                    });
                    Log.i(TAG, "Attached long-press listener to: " + cd + " (class=" + v.getClass().getName() + ")");
                }
            }
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            int n = g.getChildCount();
            for (int i = 0; i < n; i++) walk(g.getChildAt(i), activity, hideRepost);
        }
    }

    private static boolean isRepostView(int id) {
        if (id == View.NO_ID || repostViewIds == null) return false;
        for (int repostId : repostViewIds) {
            if (repostId != 0 && repostId == id) return true;
        }
        return false;
    }
}
