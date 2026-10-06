// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.playerui;

import android.view.View;
import android.view.ViewGroup;

import app.reseam.youtube.core.Logger;

/**
 * Resizes the play and pause button with its touch area and the row holding it, keeping YouTube's
 * proportions: a 56dp button with 8dp padding in an 80dp touch area.
 */
public final class PlayPauseButtonSize {
    private static final int DEFAULT_SIZE = 56;
    private static final int MIN_SIZE = 40;
    private static final int MAX_SIZE = 160;

    private PlayPauseButtonSize() {}

    /** Injection point, with the button once the player controls look it up. */
    public static void resize(View button, String sizeDp) {
        try {
            int size = Math.max(MIN_SIZE, Math.min(Integer.parseInt(sizeDp.trim()), MAX_SIZE));
            if (size == DEFAULT_SIZE) return;

            int buttonSize = Math.round(size * button.getResources().getDisplayMetrics().density);
            int padding = buttonSize * 8 / DEFAULT_SIZE;
            int touchSize = buttonSize * 80 / DEFAULT_SIZE;
            View touchArea = (View) button.getParent();
            View row = (View) touchArea.getParent();

            button.setPadding(padding, padding, padding, padding);
            setSize(button, buttonSize, buttonSize);
            setSize(touchArea, touchSize, touchSize);
            setSize(row, row.getLayoutParams().width, touchSize);
        } catch (RuntimeException e) {
            Logger.error(() -> "Play and pause button size failed: " + sizeDp, e);
        }
    }

    private static void setSize(View view, int width, int height) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        params.width = width;
        params.height = height;
        view.setLayoutParams(params);
    }
}
