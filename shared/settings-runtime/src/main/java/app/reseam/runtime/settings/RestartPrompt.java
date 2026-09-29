// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.runtime.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/**
 * Offers a restart after the root settings page closes with changed values, since most patches
 * read their settings once at startup. The prompt appears on the app screen the user returns to,
 * which covers every way of leaving the page without the host intercepting Back.
 */
final class RestartPrompt implements Application.ActivityLifecycleCallbacks {
    private static RestartPrompt instance;

    private WeakReference<Activity> root = new WeakReference<>(null);
    private Map<String, ?> opened;
    private boolean pending;

    private RestartPrompt() {}

    /** Called for each root page instance; the values it opened with survive recreation. */
    static void watch(Activity activity) {
        if (instance == null) {
            instance = new RestartPrompt();
            activity.getApplication().registerActivityLifecycleCallbacks(instance);
        }
        instance.root = new WeakReference<>(activity);
        if (instance.opened == null) instance.opened = new HashMap<>(ReseamSettings.prefs().getAll());
    }

    @Override
    public void onActivityPaused(Activity activity) {
        if (activity != root.get() || !activity.isFinishing()) return;
        pending = !opened.equals(ReseamSettings.prefs().getAll());
        opened = null;
    }

    @Override
    public void onActivityResumed(Activity activity) {
        if (!pending) return;
        pending = false;
        new AlertDialog.Builder(activity)
                .setTitle("Restart to apply changes?")
                .setMessage("Most settings take effect after the app restarts.")
                .setPositiveButton("Restart", (dialog, which) -> restart(activity))
                .setNegativeButton("Later", null)
                .show();
    }

    private static void restart(Context ctx) {
        Intent launch = ctx.getPackageManager().getLaunchIntentForPackage(ctx.getPackageName());
        ctx.startActivity(Intent.makeRestartActivityTask(launch.getComponent()));
        Runtime.getRuntime().exit(0);
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}

    @Override
    public void onActivityStarted(Activity activity) {}

    @Override
    public void onActivityStopped(Activity activity) {}

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

    @Override
    public void onActivityDestroyed(Activity activity) {}
}
