// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.playerui;

import android.widget.ImageView;

import app.reseam.youtube.core.Logger;
import app.reseam.youtube.core.Settings;

public final class CustomPlayerOverlayOpacity {
    private static final int DEFAULT_OPACITY = 100;

    private CustomPlayerOverlayOpacity() {}

    public static void changeOpacity(ImageView overlay) {
        if (overlay == null) return;
        final String value = Settings.getString("player_overlay_opacity", "100");
        final int opacity = parseOpacity(value);
        Logger.debug(() -> "CustomPlayerOverlayOpacity: " + opacity);
        overlay.setImageAlpha((opacity * 255) / 100);
    }

    private static int parseOpacity(String value) {
        try {
            final int opacity = Integer.parseInt(value.trim());
            if (opacity >= 0 && opacity <= 100) return opacity;
        } catch (RuntimeException ignored) {
            // The text setting is user-editable; the default is safer than a broken overlay.
        }
        Logger.debug(() -> "CustomPlayerOverlayOpacity: invalid value, using 100: " + value);
        return DEFAULT_OPACITY;
    }
}
