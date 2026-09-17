// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.miniplayer;

import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;

import app.reseam.youtube.core.Settings;
import app.reseam.youtube.core.YouTubeContext;

/** Controls for the miniplayer shipped by the current YouTube client. */
public final class Miniplayer {
    private Miniplayer() {}

    public static int width(int original) {
        try {
            int requested = Integer.parseInt(Settings.getString("miniplayer_width_dip", "192"));
            if (requested == 192) return original;
            DisplayMetrics metrics = YouTubeContext.get().getResources().getDisplayMetrics();
            int maximum = Math.max(192, (int) (metrics.widthPixels / metrics.density) - 15);
            return Math.round(Math.max(192, Math.min(requested, maximum)) * metrics.density);
        } catch (NumberFormatException ignored) {
            return original;
        }
    }

    public static void hideOverlayButton(View view) {
        if (Settings.getBoolean("hide_miniplayer_overlay_buttons", false)) hide(view);
    }

    public static void hideSubtext(View view) {
        if (Settings.getBoolean("hide_miniplayer_subtext", false)) hide(view);
    }

    private static void hide(View view) {
        if (view == null) return;
        if (view.getParent() instanceof ViewGroup) {
            ((ViewGroup) view.getParent()).removeView(view);
        } else {
            view.setVisibility(View.GONE);
        }
    }
}
