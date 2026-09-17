// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.misc;

import android.content.Intent;

import app.reseam.youtube.core.Logger;
import app.reseam.youtube.core.Settings;

/**
 * The start page is either a browse id the feed request carries or, for Shorts and Search, an
 * intent action, because those two surfaces have no browse id.
 */
public final class StartPage {
    private static final String LAUNCHER_ACTION = "android.intent.action.MAIN";

    private static boolean applied;

    private StartPage() {}

    /** Injection point, from the home feed's browse id. */
    public static String overrideBrowseId(String original) {
        String page = page();
        if (page.isEmpty() || isIntentAction(page) || !claim()) return original;
        Logger.debug(() -> "Changing browse id to " + page);
        return page;
    }

    /** Injection point, from the activity's intent handling. */
    public static void overrideIntentAction(Intent intent) {
        String page = page();
        if (page.isEmpty() || !isIntentAction(page)) return;
        // Only a cold start from the launcher; the Shorts shortcut and the widget send their own
        // action and must keep it.
        if (intent == null || !LAUNCHER_ACTION.equals(intent.getAction()) || !claim()) return;
        Logger.debug(() -> "Changing intent action to " + page);
        intent.setAction(page);
    }

    private static String page() {
        return Settings.getString("change_start_page", "");
    }

    private static boolean isIntentAction(String page) {
        return page.startsWith("com.google.android.youtube.action.");
    }

    /**
     * Without this the toolbar back button misbehaves, because the override would apply again on
     * every later feed load.
     */
    private static synchronized boolean claim() {
        if (Settings.getBoolean("change_start_page_always", false)) return true;
        if (applied) return false;
        applied = true;
        return true;
    }
}
