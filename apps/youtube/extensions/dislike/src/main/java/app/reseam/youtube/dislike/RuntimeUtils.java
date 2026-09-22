// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.dislike;

import android.content.Context;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import app.reseam.youtube.core.YouTubeContext;

/** Small runtime services for the RYD port; settings remain owned by Reseam's settings runtime. */
public final class RuntimeUtils {
    private static final ExecutorService NETWORK = Executors.newFixedThreadPool(3, runnable -> {
        Thread thread = new Thread(runnable, "Reseam-RYD");
        thread.setDaemon(true);
        return thread;
    });
    private RuntimeUtils() {}
    public static void executeOnBackgroundThread(Runnable task) { NETWORK.execute(task); }
    public static <K, V> Map<K, V> createSizeRestrictedMap(int limit) {
        return new LinkedHashMap<K, V>() {
            @Override protected boolean removeEldestEntry(Map.Entry<K, V> entry) { return size() > limit; }
        };
    }
    public static boolean containsNumber(CharSequence text) {
        for (int i = 0; i < text.length(); i++) if (Character.isDigit(text.charAt(i))) return true;
        return false;
    }
    public static boolean isRightToLeftLocale() {
        return YouTubeContext.get().getResources().getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
    }
    public static String getTextDirectionString() { return isRightToLeftLocale() ? "\u200f" : "\u200e"; }
    public static boolean isDarkModeEnabled() {
        return (YouTubeContext.get().getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }
    public static boolean isNetworkConnected() {
        ConnectivityManager connectivity = (ConnectivityManager) YouTubeContext.get().getSystemService(Context.CONNECTIVITY_SERVICE);
        return connectivity != null && connectivity.getActiveNetwork() != null;
    }
    public static void verifyOffMainThread() {
        if (Looper.myLooper() == Looper.getMainLooper()) throw new IllegalStateException("Network call on UI thread");
    }
    public static void verifyOnMainThread() {
        if (Looper.myLooper() != Looper.getMainLooper()) throw new IllegalStateException("UI operation off main thread");
    }
    public static void showToast(String message, int duration) {
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(YouTubeContext.get(), message, duration).show());
    }
    public static void showToastLong(String message) { showToast(message, Toast.LENGTH_LONG); }
    public static String str(String key, Object... args) {
        String message = switch (key) {
            case "revanced_ryd_video_likes_hidden_by_video_owner" -> "Likes hidden";
            case "revanced_ryd_failure_connection_timeout" -> "Return YouTube Dislike timed out";
            case "revanced_ryd_failure_client_rate_limit_requested" -> "Return YouTube Dislike is rate limited. Retrying later.";
            case "revanced_ryd_failure_connection_status_code" -> "Return YouTube Dislike returned HTTP %s";
            case "revanced_ryd_failure_generic" -> "Return YouTube Dislike: %s";
            case "revanced_ryd_failure_ryd_enabled_while_playing_video_then_user_voted" -> "Reopen the video before voting with Return YouTube Dislike";
            default -> key;
        };
        return args.length == 0 ? message : String.format(message, args);
    }
}
