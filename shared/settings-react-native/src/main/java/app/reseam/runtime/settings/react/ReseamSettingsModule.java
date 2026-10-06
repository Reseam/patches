// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.runtime.settings.react;

import app.reseam.runtime.settings.ReseamSettings;
import app.reseam.runtime.settings.ReseamSettingsScreen;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

/** Gives the app's JavaScript the same settings store the DEX patches read. */
public final class ReseamSettingsModule extends ReactContextBaseJavaModule {
    static final String NAME = "ReseamSettings";

    ReseamSettingsModule(ReactApplicationContext context) {
        super(context);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @ReactMethod(isBlockingSynchronousMethod = true)
    public boolean getBoolean(String key, boolean defaultValue) {
        return ReseamSettings.getBoolean(key, defaultValue);
    }

    @ReactMethod
    public void openSettings() {
        ReseamSettingsScreen.open();
    }
}
