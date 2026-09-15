// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.universal.googleads;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.google.android.gms.ads.AdLoadCallback;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.admanager.AdManagerAdRequest;

/**
 * Replaces the static full-screen loaders. Reporting no fill through the app's own callback
 * keeps the app's flow moving (reward denied, app-open skipped) instead of hanging on a load
 * that never returns.
 */
public final class AdMobLoader {
    private AdMobLoader() {}

    public static void load(Context context, String adUnitId, AdRequest request, AdLoadCallback<?> callback) {
        fail(callback);
    }

    public static void loadManager(Context context, String adUnitId, AdManagerAdRequest request, AdLoadCallback<?> callback) {
        fail(callback);
    }

    private static void fail(final AdLoadCallback<?> callback) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                callback.onAdFailedToLoad(new LoadAdError(3, "Blocked by Reseam", "com.google.android.gms.ads", null, null));
            }
        });
    }
}
