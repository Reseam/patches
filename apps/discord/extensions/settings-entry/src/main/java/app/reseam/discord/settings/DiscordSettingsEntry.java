// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.discord.settings;

import android.content.Context;

import app.reseam.runtime.settings.ReseamSettings;
import app.reseam.runtime.settings.react.ReseamSettingsPackage;

import com.facebook.react.ReactPackage;

import java.util.List;

public final class DiscordSettingsEntry {
    private DiscordSettingsEntry() {}

    public static void init(Context ctx) {
        ReseamSettings.init(ctx);
    }

    public static void addReactPackage(List<ReactPackage> packages) {
        packages.add(new ReseamSettingsPackage());
    }
}
