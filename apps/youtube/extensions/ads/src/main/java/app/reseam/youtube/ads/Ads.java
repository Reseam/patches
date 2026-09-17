// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.ads;

import android.app.Dialog;
import android.view.View;
import android.view.ViewGroup;

import app.reseam.youtube.core.Logger;
import app.reseam.youtube.litho.FilterGroup.ByteArrayFilterGroup;

/** Ad classification and view operations. The Kotlin hooks own the settings gates. */
public final class Ads {
    private static final ByteArrayFilterGroup FULLSCREEN_AD =
            new ByteArrayFilterGroup(null, false, "_interstitial");
    private static final ByteArrayFilterGroup STORE_BANNER =
            new ByteArrayFilterGroup(null, false, "gstatic.com/shopping");

    private Ads() {}

    public static void closeFullscreenAd(Dialog dialog, byte[] buffer) {
        if (dialog == null || buffer == null || !FULLSCREEN_AD.check(buffer).isFiltered()) return;
        try {
            dialog.dismiss();
        } catch (RuntimeException ex) {
            Logger.error(() -> "Could not dismiss fullscreen ad: " + ex);
        }
    }

    public static void hideView(View view) {
        if (view == null) return;
        view.setVisibility(View.GONE);
        ViewGroup.LayoutParams params = view.getLayoutParams();
        // Preserve the parent's LayoutParams subtype; a generic replacement can fail on attachment.
        if (params != null && (params.width != 0 || params.height != 0)) {
            params.width = 0;
            params.height = 0;
            view.setLayoutParams(params);
        }
    }

    public static boolean isStoreBanner(byte[] buffer) {
        return buffer != null && STORE_BANNER.check(buffer).isFiltered();
    }

    public static boolean isPlayerPopupAd(String panelId) {
        return panelId != null && (panelId.contains("PAproduct") || panelId.contains("jumpahead"));
    }
}
