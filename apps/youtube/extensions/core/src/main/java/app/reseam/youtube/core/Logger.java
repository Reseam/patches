// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.core;

import android.util.Log;

/** Logging for the YouTube extensions. Safe before {@link YouTubeContext#init} has run. */
public final class Logger {
    /** Built only when the line is actually written, so a disabled log costs nothing. */
    public interface Message {
        String build();
    }

    private static final String TAG = "Reseam";

    private Logger() {}

    public static void debug(Message message) {
        if (Settings.getBoolean("debug_logging", false)) Log.d(TAG, message.build());
    }

    public static void info(Message message) {
        Log.i(TAG, message.build());
    }

    public static void error(Message message) {
        Log.e(TAG, message.build());
    }
}
