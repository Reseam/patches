// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.buttons;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.Toast;

import java.lang.ref.WeakReference;

import app.reseam.youtube.controls.PlayerControlButton;
import app.reseam.youtube.core.Logger;
import app.reseam.youtube.core.Settings;
import app.reseam.youtube.core.YouTubeContext;
import app.reseam.youtube.video.VideoInformation;

public final class Downloads {
    private enum Downloader {
        YTDLNIS("YTDLnis", "com.deniscerri.ytdl", "https://ytdlnis.org"),
        SEAL("Seal", "com.junkfood.seal", "https://github.com/JunkFood02/Seal/releases/latest"),
        GRAYJAY("Grayjay", "com.futo.platformplayer", "https://grayjay.app"),
        LIBRETUBE("LibreTube", "com.github.libretube", "https://libretube.dev"),
        NEWPIPE("NewPipe", "org.schabi.newpipe", "https://newpipe.net"),
        PIPEPIPE("PipePipe", "InfinityLoop1309.NewPipeEnhanced", "https://pipepipe.dev"),
        TUBULAR("Tubular", "org.polymorphicshade.tubular", "https://github.com/polymorphicshade/Tubular/releases/latest");

        final String appName;
        final String packageName;
        final String downloadUrl;

        Downloader(String appName, String packageName, String downloadUrl) {
            this.appName = appName;
            this.packageName = packageName;
            this.downloadUrl = downloadUrl;
        }

        static Downloader forPackage(String packageName) {
            for (Downloader downloader : values()) {
                if (downloader.packageName.equals(packageName)) return downloader;
            }
            return null;
        }
    }

    private static WeakReference<Activity> activity = new WeakReference<>(null);

    private Downloads() {}

    public static void setMainActivity(Activity mainActivity) {
        activity = new WeakReference<>(mainActivity);
        Logger.debug(() -> "Downloads main activity captured");
    }

    public static void initialize(View root) {
        try {
            new PlayerControlButton(root, "reseam_external_download_button",
                    () -> Settings.getBoolean("external_downloader", false),
                    Downloads::onClick, null);
            Logger.debug(() -> "External downloader button initialized");
        } catch (Exception exception) {
            Logger.error(() -> "External downloader button initialization failure: " + exception);
        }
    }

    private static void onClick(View view) {
        launchExternalDownloader(VideoInformation.getVideoId(), view.getContext());
    }

    public static boolean inAppDownloadButtonOnClick(String videoId) {
        if (!Settings.getBoolean("external_downloader_action_button", false)) return false;
        final Activity mainActivity = activity.get();
        return launchExternalDownloader(videoId, mainActivity != null ? mainActivity : YouTubeContext.get());
    }

    private static boolean launchExternalDownloader(String videoId, Context context) {
        if (videoId == null || videoId.isEmpty() || context == null) return false;
        final String packageName = Settings.getString("external_downloader_package_name", "com.deniscerri.ytdl").trim();
        if (packageName.isEmpty()) return false;
        final Intent intent = new Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .setPackage(packageName)
                .putExtra(Intent.EXTRA_TEXT, "https://youtu.be/" + videoId);
        if (findActivity(context) == null) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            // An explicit launch also works when Android package visibility hides the downloader.
            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException exception) {
            showNotInstalledDialog(context, packageName);
            return true;
        } catch (SecurityException exception) {
            Logger.error(() -> "External downloader launch denied: " + exception);
            return false;
        }
    }

    private static Activity findActivity(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) return (Activity) context;
            Context base = ((ContextWrapper) context).getBaseContext();
            if (base == context) break;
            context = base;
        }
        return null;
    }

    private static void showNotInstalledDialog(Context context, String packageName) {
        final Activity current = findActivity(context);
        if (current == null || current.isFinishing() || current.isDestroyed()) {
            Toast.makeText(context, "Downloader unavailable: " + packageName, Toast.LENGTH_LONG).show();
            return;
        }
        final Downloader downloader = Downloader.forPackage(packageName);
        final AlertDialog.Builder dialog = new AlertDialog.Builder(current)
                .setTitle("App not installed")
                .setMessage(downloader != null
                        ? downloader.appName + " is not installed. Please install it."
                        : "Could not find an installed app with package name " + packageName
                        + ".\n\nCheck that the package name is correct and the app is installed.")
                .setNegativeButton(android.R.string.cancel, null);
        if (downloader != null) {
            dialog.setPositiveButton("Open website", (d, which) -> {
                final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(downloader.downloadUrl));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    current.startActivity(intent);
                } catch (ActivityNotFoundException exception) {
                    Toast.makeText(current, "No browser is available", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            dialog.setPositiveButton(android.R.string.ok, null);
        }
        dialog.show();
        Logger.debug(() -> "External downloader not installed: " + packageName);
    }
}
