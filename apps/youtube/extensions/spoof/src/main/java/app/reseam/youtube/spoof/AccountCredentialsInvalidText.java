// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.spoof;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import app.reseam.youtube.core.Logger;
import app.reseam.youtube.core.Settings;
import app.reseam.youtube.core.YouTubeContext;

/** Explains the MicroG account error when connectivity proves that the stock text is misleading. */
public final class AccountCredentialsInvalidText {
    private AccountCredentialsInvalidText() {}

    public static String getOfflineNetworkErrorString(String original) {
        if (!Settings.getBoolean("account_credentials_invalid_text", true)) return original;
        try {
            ConnectivityManager manager = (ConnectivityManager) YouTubeContext.get()
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo network = manager == null ? null : manager.getActiveNetworkInfo();
            if (network != null && network.isConnected()) {
                Logger.debug(() -> "Network appears to be online, but app is showing offline error");
                int id = YouTubeContext.get().getResources().getIdentifier(
                        "microg_offline_account_login_error", "string",
                        YouTubeContext.get().getPackageName());
                if (id != 0) return "\n" + YouTubeContext.get().getString(id);
            } else {
                Logger.debug(() -> "Network is offline");
            }
        } catch (Exception exception) {
            Logger.error(() -> "getOfflineNetworkErrorString failure: " + exception);
        }
        return original;
    }
}
