// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.runtime.settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import java.util.Map;

/** Offers a restart after the settings close with changed values, since most patches read their settings once at startup. */
final class RestartPrompt {
    private RestartPrompt() {}

    static void offer(Activity activity, Map<String, ?> opened) {
        if (activity.isFinishing() || opened.equals(ReseamSettings.prefs().getAll())) return;
        ReseamSettingsScreen.dialog(activity)
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
}
