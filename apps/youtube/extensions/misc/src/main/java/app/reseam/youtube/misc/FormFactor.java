// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.misc;

import app.reseam.youtube.core.Logger;
import app.reseam.youtube.core.Settings;

/** The form factor YouTube reports in its client info, which decides which layout it serves. */
public final class FormFactor {
    private FormFactor() {}

    /** Injection point, from the client info builder. */
    public static int formFactor(int original) {
        String setting = Settings.getString("change_form_factor", "");
        if (setting.isEmpty()) return original;
        try {
            int spoofed = Integer.parseInt(setting);
            Logger.debug(() -> "Reporting form factor " + spoofed + " instead of " + original);
            return spoofed;
        } catch (NumberFormatException ex) {
            Logger.error(() -> "Ignoring non-numeric form factor: " + setting);
            return original;
        }
    }
}
