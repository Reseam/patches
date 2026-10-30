// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("GmsCoreSupport")

package app.reseam.gmscore

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import app.reseam.bundle.Logger
import app.reseam.bundle.showToast
import app.reseam.gmscore.settings.GmsCoreStrings
import app.reseam.runtime.Export

// Replaced at patch time with the vendor group the bundle was built for.
@Export private external fun vendorGroupId(): String

// Replaced at patch time with the package name the app had before patching.
@Export private external fun originalPackageName(): String

internal fun gmsCorePackageName(): String = "${vendorGroupId()}.android.gms"

/** Restricts a media-route discovery intent to the GmsCore package chosen at patch time. */
@Export
fun restrictCastDiscovery(intent: Intent) {
    intent.setPackage(gmsCorePackageName())
}

@Export
fun check(activity: Activity) {
    val gmsCore = gmsCorePackageName()
    if (isOriginalPackageName(activity)) {
        // Root mounts keep the original package and ignore manifest changes; exiting can cause
        // relaunch loops.
        activity.showToast(GmsCoreStrings.gmscoreMountUnsupported)
    }
    if (!isInstalled(activity, gmsCore)) {
        activity.openScreen(
            Intent(activity, GmsCoreMissingActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        return
    }
    // Android Automotive offers no way to turn battery optimization off.
    val automotive = activity.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)
    if (!automotive && isBatteryOptimized(activity, gmsCore)) {
        activity.prompt(
            GmsCoreStrings.gmscoreBatteryOptimizedTitle,
            GmsCoreStrings.gmscoreBatteryOptimizedMessage,
            GmsCoreStrings.gmscoreOpenSettings,
        ) {
            activity.openBatteryOptimizationSettings()
        }
        return
    }
    if (!isProviderAvailable(activity)) {
        activity.prompt(
            GmsCoreStrings.gmscoreUnavailableTitle,
            GmsCoreStrings.gmscoreUnavailableMessage,
            GmsCoreStrings.gmscoreOpenWebsite,
        ) {
            activity.openScreen(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://dontkillmyapp.com/?app=MicroG"))
            )
        }
    }
}

@Export
fun dropNullValues(rows: MutableMap<*, *>) {
    rows.values.removeAll { it == null }
}

// Acquiring the gservices provider can start GmsCore's process.
private fun isProviderAvailable(context: Context): Boolean =
    try {
        context.contentResolver
            .acquireContentProviderClient(
                Uri.parse("content://${vendorGroupId()}.android.gsf.gservices/prefix")
            )
            .use { it != null }
    } catch (exception: SecurityException) {
        Logger.warn(exception) { "Cannot access GmsCore's gservices provider" }
        false
    }

fun isOriginalPackageName(context: Context): Boolean = originalPackageName() == context.packageName

private fun isInstalled(context: Context, packageName: String): Boolean =
    try {
        context.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

private fun isBatteryOptimized(context: Context, packageName: String): Boolean =
    (context.getSystemService(Context.POWER_SERVICE) as PowerManager?)
        ?.isIgnoringBatteryOptimizations(packageName) == false

private fun Activity.prompt(title: String, message: String, action: String, onAction: () -> Unit) {
    if (isFinishing || isDestroyed) return
    AlertDialog.Builder(this)
        .setTitle(title)
        .setMessage(message)
        .setCancelable(false)
        .setPositiveButton(action) { _, _ -> onAction() }
        .setNegativeButton(GmsCoreStrings.gmscoreIgnore) { dialog, _ -> dialog.dismiss() }
        .show()
}

private fun Activity.openBatteryOptimizationSettings() {
    // The per-app dialog accepts only the caller's package, so use the full optimization list.
    val optimizationSettings = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    openScreen(
        optimizationSettings.takeIf { it.resolveActivity(packageManager) != null }
            ?: Intent(Settings.ACTION_SETTINGS)
    )
}

internal fun Activity.openScreen(intent: Intent) {
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        showToast(GmsCoreStrings.gmscoreNoScreenHandler)
    }
}
