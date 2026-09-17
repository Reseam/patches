// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.buttons;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.view.View;
import android.widget.Toast;

import app.reseam.youtube.controls.PlayerControlButton;
import app.reseam.youtube.core.Logger;
import app.reseam.youtube.core.Settings;
import app.reseam.youtube.video.VideoInformation;

/** The two copy buttons share URL construction but have separate visibility settings. */
public final class CopyVideoUrl {
    private CopyVideoUrl() {}

    public static void initialize(View root) {
        try {
            new PlayerControlButton(root, "reseam_copy_video_url_button",
                    () -> Settings.getBoolean("copy_video_url", false),
                    view -> copy(view.getContext(), false),
                    view -> {
                        copy(view.getContext(), true);
                        return true;
                    });
            new PlayerControlButton(root, "reseam_copy_video_url_timestamp_button",
                    () -> Settings.getBoolean("copy_video_url_timestamp", true),
                    view -> copy(view.getContext(), true),
                    view -> {
                        copy(view.getContext(), false);
                        return true;
                    });
            Logger.debug(() -> "Copy video URL buttons initialized");
        } catch (Exception exception) {
            Logger.error(() -> "Copy video URL button initialization failure: " + exception);
        }
    }

    private static void copy(Context context, boolean withTimestamp) {
        try {
            final String videoId = VideoInformation.getVideoId();
            if (videoId == null || videoId.isEmpty()) return;
            final long seconds = Math.max(0, VideoInformation.getVideoTime() / 1000);
            final StringBuilder url = new StringBuilder("https://youtu.be/").append(videoId);
            if (withTimestamp && seconds > 0) url.append("?t=").append(seconds);

            final ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard == null) return;
            clipboard.setPrimaryClip(ClipData.newPlainText("YouTube URL", url.toString()));
            if (Build.VERSION.SDK_INT < 33 || withTimestamp && seconds > 0) {
                Toast.makeText(context, withTimestamp ? "Copied URL with timestamp" : "Copied video URL",
                        Toast.LENGTH_SHORT).show();
            }
            Logger.debug(() -> "Copied video URL: " + url);
        } catch (Exception exception) {
            Logger.error(() -> "Copy video URL failure: " + exception);
        }
    }
}
