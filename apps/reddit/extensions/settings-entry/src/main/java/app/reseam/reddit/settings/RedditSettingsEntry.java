// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.reddit.settings;

import android.content.Context;
import android.content.Intent;

import app.reseam.runtime.settings.ReseamSettings;

public final class RedditSettingsEntry {
    private static volatile Context appContext;

    private RedditSettingsEntry() {}

    public static void init(Context ctx) {
        appContext = ctx.getApplicationContext();
        ReseamSettings.init(ctx);
    }

    public static void open() {
        Intent intent = new Intent(appContext, RedditReseamSettingsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        appContext.startActivity(intent);
    }
}
