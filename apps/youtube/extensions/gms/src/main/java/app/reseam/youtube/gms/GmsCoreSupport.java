// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.gms;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

/**
 * A patched Google app talks to GmsCore instead of Play Services. GmsCore has to be installed,
 * and Android has to leave it running, or every signed-in request fails with an error the app
 * reports as a network problem. Both are checked after the main activity is created so the cause
 * is visible.
 */
public final class GmsCoreSupport {
    private GmsCoreSupport() {}

    /** Replaced at patch time with the vendor group the bundle was built for. */
    private static String vendorGroupId() {
        return "app.revanced";
    }

    /** Replaced at patch time with the package name the app had before patching. */
    private static String originalPackageName() {
        return "com.google.android.youtube";
    }

    private static String gmsCorePackageName() {
        return vendorGroupId() + ".android.gms";
    }

    /** Injection point, after the main activity's onCreate. */
    public static void check(Activity activity) {
        String gmsCore = gmsCorePackageName();

        if (isOriginalPackageName(activity)) {
            // A root mount keeps the original package name and ignores the manifest changes GmsCore
            // support depends on. Exiting here would leave some devices relaunching a hung app.
            Toast.makeText(activity, "The GmsCore support patch breaks mount installations", Toast.LENGTH_LONG).show();
        }

        if (!isInstalled(activity, gmsCore)) {
            prompt(
                    activity,
                    "GmsCore is not installed",
                    "This app was patched to sign in through GmsCore (" + gmsCore + "), which is not "
                            + "installed. Install it, open it once and grant the permissions it asks for.",
                    "Get GmsCore",
                    () -> open(activity, Uri.parse("https://github.com/revanced/gmscore/releases/latest")));
            return;
        }

        // Android Automotive offers no way to turn battery optimization off.
        boolean automotive = activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE);
        if (!automotive && isBatteryOptimized(activity, gmsCore)) {
            prompt(
                    activity,
                    "GmsCore is battery optimized",
                    "Android will stop GmsCore in the background, which signs you out and breaks "
                            + "notifications. Turn battery optimization off for GmsCore.",
                    "Open settings",
                    () -> openBatteryOptimizationSettings(activity));
            return;
        }

        if (!isProviderAvailable(activity)) {
            prompt(
                    activity,
                    "GmsCore is unavailable",
                    "The app cannot access GmsCore. Open GmsCore and check its permissions and "
                            + "background activity settings.",
                    "Open website",
                    () -> open(activity, Uri.parse("https://dontkillmyapp.com/?app=MicroG")));
        }
    }

    /** Check whether GmsCore's gservices provider is accessible. Acquisition can start its process. */
    private static boolean isProviderAvailable(Context context) {
        Uri provider = Uri.parse("content://" + vendorGroupId() + ".android.gsf.gservices/prefix");
        try (ContentProviderClient client = context.getContentResolver().acquireContentProviderClient(provider)) {
            return client != null;
        } catch (SecurityException ex) {
            Log.w("Reseam", "Cannot access GmsCore's gservices provider", ex);
            return false;
        }
    }

    /** Whether the app still runs under the package name it was published with. */
    public static boolean isOriginalPackageName(Context context) {
        return originalPackageName().equals(context.getPackageName());
    }

    private static boolean isInstalled(Context context, String packageName) {
        try {
            context.getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException ex) {
            return false;
        }
    }

    private static boolean isBatteryOptimized(Context context, String packageName) {
        PowerManager power = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        return power != null && !power.isIgnoringBatteryOptimizations(packageName);
    }

    private static void prompt(Activity activity, String title, String message, String action, Runnable onAction) {
        if (activity.isFinishing() || activity.isDestroyed()) return;
        new AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(action, (dialog, which) -> onAction.run())
                .setNegativeButton("Ignore", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private static void open(Activity activity, Uri uri) {
        open(activity, new Intent(Intent.ACTION_VIEW, uri));
    }

    @SuppressWarnings("BatteryLife")
    private static void openBatteryOptimizationSettings(Activity activity) {
        // The per-app request dialog only accepts the caller's own package, so GmsCore is
        // reached through the full battery optimization list instead.
        Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
        if (intent.resolveActivity(activity.getPackageManager()) == null) {
            intent = new Intent(Settings.ACTION_SETTINGS);
        }
        open(activity, intent);
    }

    private static void open(Activity activity, Intent intent) {
        try {
            activity.startActivity(intent);
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(activity, "No app is available to open this screen", Toast.LENGTH_LONG).show();
        }
    }
}
