// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.misc;

import android.app.Activity;
import android.app.AlertDialog;

import java.net.InetAddress;

import app.reseam.youtube.core.Logger;

/**
 * A blocklist that sinkholes s.youtube.com stops watch history from being recorded, and the app
 * gives no sign of it. Resolving the domain at launch turns a silent failure into a dialog.
 */
public final class WatchHistoryDns {
    private static final String HISTORY_ENDPOINT = "s.youtube.com";
    private static final String CONTROL_DOMAIN = "youtube.com";

    private WatchHistoryDns() {}

    /** Injection point, from the main activity; the patch gates the call on the setting. */
    public static void check(Activity activity) {
        new Thread(() -> {
            // Checked twice, alternating with a domain that must resolve, so a flaky connection
            // or a device that just lost the network does not raise a false alarm.
            if (!resolves(CONTROL_DOMAIN) || resolves(HISTORY_ENDPOINT)
                    || !resolves(CONTROL_DOMAIN) || resolves(HISTORY_ENDPOINT)) {
                return;
            }
            activity.runOnUiThread(() -> warn(activity));
        }, "Reseam watch history DNS").start();
    }

    private static void warn(Activity activity) {
        if (activity.isFinishing() || activity.isDestroyed()) return;
        new AlertDialog.Builder(activity)
                .setTitle("Watch history is blocked")
                .setMessage("The device DNS server does not resolve " + HISTORY_ENDPOINT
                        + ", so YouTube cannot record what you watch and recommendations will not"
                        + " improve. A content blocker or private DNS profile is usually the cause.")
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private static boolean resolves(String host) {
        try {
            InetAddress address = InetAddress.getByName(host);
            String resolved = address.getHostAddress();
            if (address.isLoopbackAddress() || address.isAnyLocalAddress()) {
                Logger.debug(() -> host + " resolves to " + resolved);
                return false;
            }
            return true;
        } catch (Exception ex) {
            Logger.debug(() -> host + " does not resolve");
            return false;
        }
    }
}
