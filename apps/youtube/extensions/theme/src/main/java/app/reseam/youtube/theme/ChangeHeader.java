// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.theme;

import android.content.Context;

import app.reseam.youtube.core.Logger;
import app.reseam.youtube.core.Settings;

public final class ChangeHeader {
    private ChangeHeader() {}

    public static int getHeaderAttributeId(int original) {
        String selected = Settings.getString("change_header_logo", "default");
        String name;
        if ("premium".equals(selected)) name = "ytPremiumWordmarkHeader";
        else if ("reseam".equals(selected)) name = "reseam_header";
        else return original;
        Context context = app.reseam.youtube.core.YouTubeContext.get();
        int id = context.getResources().getIdentifier(name, "attr", context.getPackageName());
        Logger.debug(() -> "Header logo: " + selected + " -> " + name);
        return id == 0 ? original : id;
    }
}
