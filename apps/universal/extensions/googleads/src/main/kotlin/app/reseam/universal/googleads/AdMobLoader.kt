// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("AdMobLoader")

package app.reseam.universal.googleads

import android.content.Context
import app.reseam.bundle.postToMainThread
import app.reseam.runtime.Export
import com.google.android.gms.ads.AdLoadCallback
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.admanager.AdManagerAdRequest

// Reported asynchronously, as AdMob does, so the app's full-screen ad flow continues.
@Export
fun load(context: Context, adUnitId: String, request: AdRequest, callback: AdLoadCallback<*>) =
    fail(callback)

@Export
fun loadManager(
    context: Context,
    adUnitId: String,
    request: AdManagerAdRequest,
    callback: AdLoadCallback<*>,
) = fail(callback)

private fun fail(callback: AdLoadCallback<*>) {
    postToMainThread {
        callback.onAdFailedToLoad(
            LoadAdError(3, "Blocked by Reseam", "com.google.android.gms.ads", null, null)
        )
    }
}
