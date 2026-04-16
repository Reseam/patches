// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later


package app.reseam.instagram.download;

import android.util.Log;

final class Logcat {
    private static final String TAG = "Reseam";

    private Logcat() {}

    static void d(String message) {
        Log.d(TAG, message);
    }

    static void w(String message) {
        Log.w(TAG, message);
    }

    static void e(String message) {
        Log.e(TAG, message);
    }

    static void e(String message, Throwable throwable) {
        Log.e(TAG, message + ": " + throwable, throwable);
    }
}
