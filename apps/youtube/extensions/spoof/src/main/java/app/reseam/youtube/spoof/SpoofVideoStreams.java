// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.spoof;

import android.net.Uri;
import android.text.TextUtils;

import java.util.Arrays;
import java.util.Map;

import app.reseam.youtube.core.Logger;
import app.reseam.youtube.core.Settings;
import app.reseam.youtube.web.WebPlayer;

/** Runtime hooks for replacing the app's player response with a compatible client response. */
public final class SpoofVideoStreams {
    private SpoofVideoStreams() {}

    public static void setClientOrderToUse() {
        ClientType client = ClientType.fromSetting(Settings.getString("spoof_video_streams_client", "web"));
        StreamingDataRequest.setClientOrder(client, Arrays.asList(ClientType.values()),
                true, Settings.getBoolean("force_original_audio", true));
        if (isSpoofingEnabled() && StreamingDataRequest.usesWebPlayer()) WebPlayer.warmUp();
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

    public static void fetchStreams(String url, Map<String, String> requestHeaders, byte[] requestBody) {
        if (!isSpoofingEnabled() || url == null) return;
        try {
            Uri uri = Uri.parse(url);
            String path = uri.getPath();
            if (path == null || !path.contains("player")) return;
            if (path.contains("get_drm_license") || path.contains("heartbeat") || path.contains("ad_break")) return;
            String videoId = uri.getQueryParameter("id");
            if (videoId == null || videoId.isEmpty()) return;
            StreamingDataRequest.fetchRequest(videoId, requestHeaders, requestBody);
        } catch (Exception exception) {
            Logger.error(() -> "Spoof stream URL failure: " + exception);
        }
    }

    /** Waits for the replacement and retains its provenance until the native parser installs it. */
    public static StreamingData getStreamingData(String videoId) {
        if (!isSpoofingEnabled() || videoId == null) return null;
        StreamingData response = StreamingDataRequest.get(videoId);
        if (response != null) Logger.debug(() -> "Overriding video stream: " + videoId);
        else Logger.debug(() -> "Not overriding streaming data (video stream is null): " + videoId);
        return response;
    }

    /** The protobuf bytes consumed by the app's own parser. */
    public static byte[] getStreamingDataBytes(StreamingData data) {
        return data.response;
    }

    /** Called only after the native streaming-data field has been replaced successfully. */
    public static void onStreamingDataInstalled(String videoId, StreamingData data) {
        InstalledStreams.installed(videoId, data);
    }

    /** A failed replacement leaves this response on the native path; do not report an old client. */
    public static void onNativeStreamingDataInstalled(String videoId) {
        InstalledStreams.nativeInstalled(videoId);
    }

    /** Tracks foreground and background playback for response-specific diagnostics. */
    public static void onVideoChanged(String videoId) {
        InstalledStreams.videoChanged(videoId);
    }

    public static byte[] removeVideoPlaybackPostBody(Uri uri, int method, byte[] postData) {
        if (method == 2 && uri != null && InstalledStreams.owns(uri)) {
            Logger.debug(() -> "Removed Android video playback POST body for installed spoofed stream");
            return null;
        }
        return postData;
    }

    /** Direct media URLs use GET; keep the native method for every other stream. */
    public static int rewriteVideoPlaybackMethod(Uri uri, int method) {
        return method == 2 && uri != null && InstalledStreams.owns(uri) ? 1 : method;
    }

    public static String appendSpoofedClient(String original) {
        if (isSpoofingEnabled() && Settings.getBoolean("spoof_video_streams_stats_for_nerds", true)
                && !TextUtils.isEmpty(original)) {
            return "\u202D" + original + "\u2009(" + InstalledStreams.clientName() + ")";
        }
        return original;
    }
}
