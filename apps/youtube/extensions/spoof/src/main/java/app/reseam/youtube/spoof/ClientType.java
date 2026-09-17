// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.spoof;

import android.os.Build;

import java.util.Locale;

/** YouTube clients whose player responses can replace the app's own streams. */
enum ClientType {
    // Used by most open-source stream extractors since 2024. Versions like 20.44.38 do not work.
    ANDROID_REEL("android_reel", "ANDROID", "com.google.android.youtube",
            Build.MANUFACTURER, Build.MODEL, "Android", Build.VERSION.RELEASE,
            String.valueOf(Build.VERSION.SDK_INT), Build.ID, "20.26.46",
            true, true, false, "Android Reel"),
    ANDROID_REEL_NO_AUTH("android_reel_no_auth", "ANDROID", "com.google.android.youtube",
            Build.MANUFACTURER, Build.MODEL, "Android", Build.VERSION.RELEASE,
            String.valueOf(Build.VERSION.SDK_INT), Build.ID, "20.26.46",
            false, true, false, "Android Reel no auth"),
    ANDROID_VR_1_61("android_vr_1_61", "ANDROID_VR", "com.google.android.apps.youtube.vr.oculus",
            "Oculus", "Quest 3", "Android", "12", "32", "SQ3A.220605.009.A1", "1.61.48",
            false, false, true, "Android VR 1.61"),
    ANDROID_VR_1_43("android_vr_1_43", "ANDROID_VR", "com.google.android.apps.youtube.vr.oculus",
            "Oculus", "Quest 3", "Android", "12", "32", "SQ3A.220605.009.A1", "1.43.32",
            false, false, true, "Android VR 1.43"),
    ANDROID_CREATOR("android_creator", "ANDROID_CREATOR", "com.google.android.apps.youtube.creator",
            "Google", "Pixel 9 Pro Fold", "Android", "15", "35", "AP3A.241005.015.A2", "23.47.101",
            true, false, true, "Android Studio"),
    VISIONOS("visionos", "VISIONOS", "Apple", "RealityDevice14,1", "visionOS", "1.3.21O771", "0.1",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Safari/605.1.15",
            false, false, true, "visionOS");

    final String settingValue;
    final String clientName;
    final String deviceMake;
    final String deviceModel;
    final String osName;
    final String osVersion;
    /** Null for a client that is not Android. */
    final String androidSdkVersion;
    final String clientVersion;
    final String userAgent;
    /** Whether the request carries the signed-in account; such a client is skipped when signed out. */
    final boolean useAuth;
    final boolean supportsMultiAudioTracks;
    /** The /player endpoint, or /reel/reel_item_watch when false. */
    final boolean usePlayerEndpoint;
    /** Shown in stats for nerds. */
    final String friendlyName;

    ClientType(String settingValue, String clientName, String packageName, String deviceMake,
               String deviceModel, String osName, String osVersion, String androidSdkVersion,
               String buildId, String clientVersion, boolean useAuth,
               boolean supportsMultiAudioTracks, boolean usePlayerEndpoint, String friendlyName) {
        this(settingValue, clientName, deviceMake, deviceModel, osName, osVersion, androidSdkVersion,
                clientVersion, String.format("%s/%s (Linux; U; Android %s; %s; %s; Build/%s)",
                        packageName, clientVersion, osVersion, Locale.getDefault(), deviceModel, buildId),
                useAuth, supportsMultiAudioTracks, usePlayerEndpoint, friendlyName);
    }

    ClientType(String settingValue, String clientName, String deviceMake, String deviceModel,
               String osName, String osVersion, String clientVersion, String userAgent,
               boolean useAuth, boolean supportsMultiAudioTracks, boolean usePlayerEndpoint,
               String friendlyName) {
        this(settingValue, clientName, deviceMake, deviceModel, osName, osVersion, null,
                clientVersion, userAgent, useAuth, supportsMultiAudioTracks, usePlayerEndpoint, friendlyName);
    }

    ClientType(String settingValue, String clientName, String deviceMake, String deviceModel,
               String osName, String osVersion, String androidSdkVersion, String clientVersion,
               String userAgent, boolean useAuth, boolean supportsMultiAudioTracks,
               boolean usePlayerEndpoint, String friendlyName) {
        this.settingValue = settingValue;
        this.clientName = clientName;
        this.deviceMake = deviceMake;
        this.deviceModel = deviceModel;
        this.osName = osName;
        this.osVersion = osVersion;
        this.androidSdkVersion = androidSdkVersion;
        this.clientVersion = clientVersion;
        this.userAgent = userAgent;
        this.useAuth = useAuth;
        this.supportsMultiAudioTracks = supportsMultiAudioTracks;
        this.usePlayerEndpoint = usePlayerEndpoint;
        this.friendlyName = friendlyName;
    }

    static ClientType fromSetting(String value) {
        for (ClientType client : values()) {
            if (client.settingValue.equals(value)) return client;
        }
        return ANDROID_REEL_NO_AUTH;
    }
}
