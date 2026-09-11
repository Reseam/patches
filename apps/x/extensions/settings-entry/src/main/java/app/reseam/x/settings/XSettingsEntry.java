// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.x.settings;

import android.content.Context;
import android.content.Intent;

import app.reseam.runtime.settings.ReseamSettings;

public final class XSettingsEntry {
    private static volatile Context appContext;

    private XSettingsEntry() {}

    public static void init(Context ctx) {
        appContext = ctx.getApplicationContext();
        ReseamSettings.init(ctx);
    }

    public static void open() {
        Intent intent = new Intent(appContext, XReseamSettingsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        appContext.startActivity(intent);
    }
}
