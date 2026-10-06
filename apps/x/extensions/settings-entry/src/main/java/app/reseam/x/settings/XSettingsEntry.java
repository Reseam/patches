// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.x.settings;

import android.content.Context;

import app.reseam.runtime.settings.ReseamSettings;

public final class XSettingsEntry {
    private XSettingsEntry() {}

    public static void init(Context ctx) {
        ReseamSettings.init(ctx);
    }
}
