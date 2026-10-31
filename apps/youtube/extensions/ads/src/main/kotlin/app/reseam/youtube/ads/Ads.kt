// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

@file:JvmName("Ads")

package app.reseam.youtube.ads

import android.app.Dialog
import app.reseam.bundle.Logger
import app.reseam.runtime.Export
import app.reseam.youtube.litho.FilterGroup.ByteArrayFilterGroup

private val fullscreenAd = ByteArrayFilterGroup(null, "_interstitial")
private val storeBanner = ByteArrayFilterGroup(null, "gstatic.com/shopping")
private val playerPopupAds = listOf("PAproduct", "jumpahead")

@Export
fun closeFullscreenAd(dialog: Dialog?, buffer: ByteArray?) {
    if (dialog == null || buffer == null || !fullscreenAd.check(buffer).isFiltered) return
    try {
        dialog.dismiss()
    } catch (exception: RuntimeException) {
        Logger.error(exception) { "Could not dismiss fullscreen ad" }
    }
}

@Export fun isStoreBanner(buffer: ByteArray?): Boolean = storeBanner.check(buffer).isFiltered

@Export
fun isPlayerPopupAd(panelId: String?): Boolean =
    panelId != null && playerPopupAds.any { it in panelId }
