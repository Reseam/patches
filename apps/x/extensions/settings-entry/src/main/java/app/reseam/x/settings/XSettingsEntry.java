// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.x.settings;

import android.content.Context;

import app.reseam.runtime.settings.ReseamSettings;

public final class XSettingsEntry {
    private XSettingsEntry() {}

    public static void init(Context ctx) {
        ReseamSettings.init(ctx);
    }
}
