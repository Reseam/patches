// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.discord.settings;

import android.content.Context;
import android.content.Intent;

import app.reseam.runtime.settings.ReseamSettings;
import app.reseam.runtime.settings.react.ReseamSettingsPackage;

import com.facebook.react.ReactPackage;

import java.util.List;

public final class DiscordSettingsEntry {
    private static volatile Context appContext;

    private DiscordSettingsEntry() {}

    public static void init(Context ctx) {
        appContext = ctx.getApplicationContext();
        ReseamSettings.init(ctx);
    }

    public static void addReactPackage(List<ReactPackage> packages) {
        packages.add(new ReseamSettingsPackage(DiscordSettingsEntry::open));
    }

    private static void open() {
        Intent intent = new Intent(appContext, DiscordReseamSettingsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        appContext.startActivity(intent);
    }
}
