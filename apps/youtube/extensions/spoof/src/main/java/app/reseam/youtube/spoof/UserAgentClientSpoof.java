// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.spoof;

import app.reseam.youtube.core.Logger;
import app.reseam.youtube.core.Settings;

/** Keeps generated user agents tied to the package YouTube's servers know. */
public final class UserAgentClientSpoof {
    private UserAgentClientSpoof() {}

    public static String rewritePackageName(String original) {
        if (!Settings.getBoolean("user_agent_client_spoof", true)) return original;
        Logger.debug(() -> "User-agent package name spoofed to com.google.android.youtube");
        return "com.google.android.youtube";
    }
}
