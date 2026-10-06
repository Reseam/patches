// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later


package app.reseam.runtime.settings;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

public final class ReseamSettings {
    private static final String PREFS_NAME = "reseam_settings";
    private static volatile Context appContext;

    private ReseamSettings() {}

    public static void init(Context ctx) {
        if (ctx == null) return;
        Context app = ctx.getApplicationContext();
        appContext = app == null ? ctx : app;
        if (appContext instanceof Application) SettingsPanel.track((Application) appContext);
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        SharedPreferences prefs = prefs();
        return prefs == null ? defaultValue : prefs.getBoolean(key, defaultValue);
    }

    public static void setBoolean(String key, boolean value) {
        SharedPreferences prefs = prefs();
        if (prefs != null) prefs.edit().putBoolean(key, value).apply();
    }

    public static String getString(String key, String defaultValue) {
        SharedPreferences prefs = prefs();
        return prefs == null ? defaultValue : prefs.getString(key, defaultValue);
    }

    /** The stored value of a choice setting if its schema lists it, and the schema default otherwise. */
    public static String getChoice(String key) {
        Context ctx = appContext;
        if (ctx == null) throw new IllegalStateException("ReseamSettings.init has not run");
        SettingsSchema.Choice choice = (SettingsSchema.Choice) SettingsSchema.load(ctx).setting(key);
        return choice.values[choice.index(getString(key, choice.defaultValue))];
    }

    public static void setString(String key, String value) {
        SharedPreferences prefs = prefs();
        if (prefs != null) prefs.edit().putString(key, value).apply();
    }

    public static SharedPreferences prefs() {
        Context ctx = appContext;
        return ctx == null ? null : ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
