// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.spoof;

import app.reseam.youtube.core.Logger;
import app.reseam.youtube.core.Settings;

/** Keeps generated user agents tied to the package the app's servers know. */
public final class UserAgentClientSpoof {
    private UserAgentClientSpoof() {}

    /** Injection point, from the user-agent builders; {@code stockPackageName} is the app's Play Store package. */
    public static String rewritePackageName(String original, String stockPackageName) {
        if (!Settings.getBoolean("user_agent_client_spoof", true)) return original;
        Logger.debug(() -> "User-agent package name spoofed to " + stockPackageName);
        return stockPackageName;
    }
}
