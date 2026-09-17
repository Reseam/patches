// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.core;

import android.content.Context;

import app.reseam.runtime.settings.ReseamSettings;

/**
 * The application context, held for extension code that runs where none is passed in: resource
 * lookups, view construction, toasts. Set from the app's own {@code Application.onCreate}, so it is
 * available before any hooked code runs.
 */
public final class YouTubeContext {
    private static volatile Context context;

    private YouTubeContext() {}

    /** Injection point, from the application entry point. */
    public static void init(Context applicationContext) {
        context = applicationContext;
        ReseamSettings.init(applicationContext);
        Logger.info(() -> "Reseam extensions loaded");
    }

    public static Context get() {
        Context current = context;
        if (current == null) throw new IllegalStateException("YouTubeContext.init has not run");
        return current;
    }
}
