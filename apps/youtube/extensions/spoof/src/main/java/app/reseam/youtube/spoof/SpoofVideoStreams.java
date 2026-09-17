// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.spoof;

import android.net.Uri;
import android.text.TextUtils;

import java.util.Arrays;
import java.util.Map;

import app.reseam.youtube.core.Logger;
import app.reseam.youtube.core.Settings;

/** Runtime hooks for replacing the app's player response with a compatible client response. */
public final class SpoofVideoStreams {
    private static final String INTERNET_CONNECTION_CHECK_URI_STRING = "https://www.youtube.com/generate_204";
    private static final Uri INTERNET_CONNECTION_CHECK_URI = Uri.parse(INTERNET_CONNECTION_CHECK_URI_STRING);

    private SpoofVideoStreams() {}

    /** VR 1.61 carries AV1 and is used in place of VR 1.43 when AV1 is allowed and AVC is not forced. */
    public static void setClientOrderToUse() {
        ClientType client = ClientType.fromSetting(Settings.getString("spoof_video_streams_client", "android_reel_no_auth"));
        if (client == ClientType.ANDROID_VR_1_43 && Settings.getBoolean("spoof_video_streams_av1", false)
                && !Settings.getBoolean("force_avc_codec", false)) {
            client = ClientType.ANDROID_VR_1_61;
        }
        // Reel requests can take a minute to start playback, so the reel client is used only when chosen.
        StreamingDataRequest.setClientOrder(client,
                Arrays.asList(ClientType.ANDROID_CREATOR, ClientType.ANDROID_VR_1_43, ClientType.VISIONOS),
                true, Settings.getBoolean("force_original_audio", true));
    }

    public static boolean isSpoofingEnabled() {
        return Settings.getBoolean("spoof_video_streams", true);
    }

    public static String rewriteClientContextOsName(String original) {
        if (!isSpoofingEnabled()) return original;
        String rewritten = StreamingDataRequest.getClientOsName();
        if (rewritten != null && !rewritten.equals(original)) {
            Logger.debug(() -> "Client-context OS name spoofed to " + rewritten);
            return rewritten;
        }
        return original;
    }

    public static Uri blockGetWatchRequest(Uri original) {
        if (isSpoofingEnabled() && original != null) {
            String path = original.getPath();
            if (path != null && path.contains("get_watch")) {
                Logger.debug(() -> "Blocking 'get_watch' by returning YouTube connection-check URI");
                return INTERNET_CONNECTION_CHECK_URI;
            }
        }
        return original;
    }

    public static String blockGetAttRequest(String original) {
        if (isSpoofingEnabled() && original != null && containsPath(original, "att/get")) {
            Logger.debug(() -> "Blocking 'att/get' by returning YouTube connection-check URI");
            return INTERNET_CONNECTION_CHECK_URI_STRING;
        }
        return original;
    }

    public static String blockInitPlaybackRequest(String original) {
        if (isSpoofingEnabled() && original != null && containsPath(original, "initplayback")) {
            Logger.debug(() -> "Blocking 'initplayback' by returning YouTube connection-check URI");
            return INTERNET_CONNECTION_CHECK_URI_STRING;
        }
        return original;
    }

    public static void fetchStreams(String url, Map<String, String> requestHeaders) {
        if (!isSpoofingEnabled() || url == null) return;
        try {
            Uri uri = Uri.parse(url);
            String path = uri.getPath();
            if (path == null || !path.contains("player")) return;
            if (path.contains("get_drm_license") || path.contains("heartbeat")
                    || path.contains("refresh") || path.contains("ad_break")) return;
            String videoId = uri.getQueryParameter("id");
            if (videoId == null || videoId.isEmpty()) return;
            StreamingDataRequest.fetchRequest(videoId, requestHeaders);
        } catch (Exception exception) {
            Logger.error(() -> "Spoof stream URL failure: " + exception);
        }
    }

    public static byte[] getStreamingData(String videoId) {
        if (!isSpoofingEnabled() || videoId == null) return null;
        byte[] response = StreamingDataRequest.get(videoId);
        if (response != null) Logger.debug(() -> "Overriding video stream: " + videoId);
        else Logger.debug(() -> "Not overriding streaming data (video stream is null): " + videoId);
        return response;
    }

    public static byte[] removeVideoPlaybackPostBody(Uri uri, int method, byte[] postData) {
        if (isSpoofingEnabled() && method == 2 && uri != null) {
            String path = uri.getPath();
            if (path != null && path.contains("videoplayback")) {
                Logger.debug(() -> "Removed Android video playback POST body for spoofed stream");
                return null;
            }
        }
        return postData;
    }

    public static boolean fixHLSCurrentTime(boolean original) {
        return isSpoofingEnabled() ? false : original;
    }

    public static boolean disableSABR() {
        return isSpoofingEnabled();
    }

    public static boolean useMediaFetchHotConfigReplacement(boolean original) {
        if (original) Logger.debug(() -> "useMediaFetchHotConfigReplacement is set on");
        return isSpoofingEnabled() ? false : original;
    }

    public static boolean usePlaybackStartFeatureFlag(boolean original) {
        if (original) Logger.debug(() -> "usePlaybackStartFeatureFlag is set on");
        return isSpoofingEnabled() ? false : original;
    }

    public static String appendSpoofedClient(String original) {
        if (isSpoofingEnabled() && Settings.getBoolean("spoof_video_streams_stats_for_nerds", true)
                && !TextUtils.isEmpty(original)) {
            return "\u202D" + original + "\u2009(" + StreamingDataRequest.getLastSpoofedClientName() + ")";
        }
        return original;
    }

    private static boolean containsPath(String value, String path) {
        try {
            String originalPath = Uri.parse(value).getPath();
            return originalPath != null && originalPath.contains(path);
        } catch (Exception exception) {
            return false;
        }
    }
}
