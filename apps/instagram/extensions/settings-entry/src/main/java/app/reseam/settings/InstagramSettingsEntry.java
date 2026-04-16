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
    private static boolean hookInstalled = false;

    private InstagramSettingsEntry() {}

    public static void init(Context ctx) {
        Log.i(TAG, "InstagramSettingsEntry.init() called");
        ReseamSettings.init(ctx);
        Context app = ctx == null ? null : ctx.getApplicationContext();
        if (app instanceof Application) {
            installLongPressHook((Application) app);
        }
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
                walk(decor, activity);
            }
        });
        walk(decor, activity);
    }

    private static void walk(View v, Activity activity) {
        if (v == null) return;
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
            for (int i = 0; i < n; i++) walk(g.getChildAt(i), activity);
        }
    }
}
