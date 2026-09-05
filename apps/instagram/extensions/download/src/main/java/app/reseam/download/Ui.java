// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later


package app.reseam.instagram.download;

import android.content.Context;
import android.widget.Toast;

import app.reseam.runtime.settings.ReseamSettings;

final class Ui {
    private static final String SHOW_TOAST_KEY = "download.show_toast";

    private Ui() {}

    static void showToast(Context context, String message) {
        try {
            if (!ReseamSettings.getBoolean(SHOW_TOAST_KEY, true)) return;
            Context safe = ContextResolver.safe(context);
            if (safe != null) Toast.makeText(safe, message, Toast.LENGTH_SHORT).show();
        } catch (Throwable t) {
            Logcat.e("showToast failed", t);
        }
    }
}
