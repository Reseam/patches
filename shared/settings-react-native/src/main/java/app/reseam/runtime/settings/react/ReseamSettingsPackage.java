// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.runtime.settings.react;

import com.facebook.react.BaseReactPackage;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.module.model.ReactModuleInfo;
import com.facebook.react.module.model.ReactModuleInfoProvider;

import java.util.Collections;

/** Registers {@link ReseamSettingsModule}; a settings entry patch adds it to the app's package list. */
public final class ReseamSettingsPackage extends BaseReactPackage {
    private final Runnable openSettings;

    public ReseamSettingsPackage(Runnable openSettings) {
        this.openSettings = openSettings;
    }

    @Override
    public NativeModule getModule(String name, ReactApplicationContext context) {
        return ReseamSettingsModule.NAME.equals(name) ? new ReseamSettingsModule(context, openSettings) : null;
    }

    @Override
    public ReactModuleInfoProvider getReactModuleInfoProvider() {
        ReactModuleInfo info = new ReactModuleInfo(ReseamSettingsModule.NAME,
                ReseamSettingsModule.class.getName(), false, false, false, false);
        return () -> Collections.singletonMap(ReseamSettingsModule.NAME, info);
    }
}
