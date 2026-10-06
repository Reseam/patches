// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.spoof;

/** YouTube clients whose player responses can replace the app's own streams. */
enum ClientType {
    // A web client: its stream URLs need the web player's solved challenges and a BotGuard PoToken.
    TV_SIMPLY("tv_simply", "TVHTML5_SIMPLY", "1.0", null, null, null, null,
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36",
            "TV Simply", true),
    // Needs neither, but "made for kids" videos are unavailable.
    VISIONOS("visionos", "VISIONOS", "0.1", "Apple", "RealityDevice14,1", "visionOS", "1.3.21O771",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Safari/605.1.15",
            "visionOS", false);

    final String settingValue;
    final String clientName;
    final String clientVersion;
    final String deviceMake;
    final String deviceModel;
    final String osName;
    final String osVersion;
    final String userAgent;
    /** Shown in stats for nerds. */
    final String friendlyName;
    /** Whether its stream URLs need {@link app.reseam.youtube.web.WebPlayer}. */
    final boolean usesWebPlayer;

    ClientType(String settingValue, String clientName, String clientVersion, String deviceMake,
               String deviceModel, String osName, String osVersion, String userAgent,
               String friendlyName, boolean usesWebPlayer) {
        this.settingValue = settingValue;
        this.clientName = clientName;
        this.clientVersion = clientVersion;
        this.deviceMake = deviceMake;
        this.deviceModel = deviceModel;
        this.osName = osName;
        this.osVersion = osVersion;
        this.userAgent = userAgent;
        this.friendlyName = friendlyName;
        this.usesWebPlayer = usesWebPlayer;
    }

    static ClientType fromSetting(String value) {
        for (ClientType client : values()) {
            if (client.settingValue.equals(value)) return client;
        }
        return TV_SIMPLY;
    }
}
