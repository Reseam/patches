// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.playerui;

import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import app.reseam.youtube.core.Logger;
import app.reseam.youtube.core.Settings;

public final class HidePlayerOverlayButtons {
    private static final String SETTING_CAPTIONS = "hide_captions_button";
    private static final String SETTING_CAST = "hide_cast_button";
    private static final String SETTING_COLLAPSE = "hide_collapse_button";
    private static final String SETTING_FULLSCREEN = "hide_fullscreen_button";
    private static final String SETTING_BACKGROUND = "hide_player_control_buttons_background";
    private static final String SETTING_PREVIOUS_NEXT = "hide_player_previous_next_buttons";

    private HidePlayerOverlayButtons() {}

    public static int getCastButtonOverrideV2(int original) {
        final boolean hide = Settings.getBoolean(SETTING_CAST, true);
        Logger.debug(() -> "HidePlayerOverlayButtons.castVisibility: " + hide);
        return hide ? View.GONE : original;
    }

    public static boolean getCastButtonOverrideV2(boolean original) {
        final boolean hide = Settings.getBoolean(SETTING_CAST, true);
        Logger.debug(() -> "HidePlayerOverlayButtons.castEnabled: " + hide);
        return hide ? false : original;
    }

    public static void hideCaptionsButton(ImageView button) {
        final boolean hide = Settings.getBoolean(SETTING_CAPTIONS, false);
        Logger.debug(() -> "HidePlayerOverlayButtons.captions: " + hide);
        if (hide && button != null) button.setVisibility(View.GONE);
    }

    public static void hideCollapseButton(ImageView button) {
        final boolean hide = Settings.getBoolean(SETTING_COLLAPSE, false);
        Logger.debug(() -> "HidePlayerOverlayButtons.collapse: " + hide);
        if (!hide || button == null) return;

        button.setImageResource(android.R.color.transparent);
        button.setImageAlpha(0);
        button.setEnabled(false);
        if (button.getLayoutParams() instanceof RelativeLayout.LayoutParams) {
            button.getLayoutParams().width = 0;
            button.getLayoutParams().height = 0;
        }
    }

    public static void setTitleAnchorStartMargin(View view) {
        final boolean hide = Settings.getBoolean(SETTING_COLLAPSE, false);
        Logger.debug(() -> "HidePlayerOverlayButtons.titleAnchor: " + hide);
        if (!hide || view == null) return;

        if (view.getLayoutParams() instanceof RelativeLayout.LayoutParams) {
            ((RelativeLayout.LayoutParams) view.getLayoutParams()).setMarginStart(0);
        }
    }

    public static void hidePreviousNextButtons(View parent) {
        final boolean hide = Settings.getBoolean(SETTING_PREVIOUS_NEXT, false);
        Logger.debug(() -> "HidePlayerOverlayButtons.previousNext: " + hide);
        if (!hide || parent == null) return;

        parent.post(() -> {
            final String packageName = parent.getContext().getPackageName();
            final int previousId = parent.getResources().getIdentifier(
                    "player_control_previous_button_touch_area", "id", packageName);
            final int nextId = parent.getResources().getIdentifier(
                    "player_control_next_button_touch_area", "id", packageName);
            remove(parent, previousId);
            remove(parent, nextId);
            Logger.debug(() -> "HidePlayerOverlayButtons.previousNext applied");
        });
    }

    private static void remove(View parent, int id) {
        if (id == 0) return;
        final View view = parent.findViewById(id);
        if (view == null) return;
        final ViewGroup group = view.getParent() instanceof ViewGroup ? (ViewGroup) view.getParent() : null;
        if (group != null) group.removeView(view);
        else view.setVisibility(View.GONE);
    }

    public static View hideFullscreenButton(View view) {
        final boolean hide = Settings.getBoolean(SETTING_FULLSCREEN, false);
        Logger.debug(() -> "HidePlayerOverlayButtons.fullscreen: " + hide);
        if (hide && view != null) {
            view.setVisibility(View.GONE);
            return null;
        }
        return view;
    }

    public static void hidePlayerControlButtonsBackground(View root) {
        if (root != null && Settings.getBoolean(SETTING_BACKGROUND, false)) clearBackgrounds(root);
    }

    private static void clearBackgrounds(View view) {
        if (view instanceof ImageView) view.setBackgroundColor(Color.TRANSPARENT);
        if (!(view instanceof ViewGroup)) return;
        final ViewGroup group = (ViewGroup) view;
        for (int i = 0; i < group.getChildCount(); i++) clearBackgrounds(group.getChildAt(i));
    }
}
