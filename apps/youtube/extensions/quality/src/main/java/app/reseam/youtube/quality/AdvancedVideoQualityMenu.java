// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.quality;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ListView;

import app.reseam.youtube.core.Logger;
import app.reseam.youtube.core.Settings;

/** Opens the detailed quality list when the quick Litho menu is drawn. */
public final class AdvancedVideoQualityMenu {
    private static volatile boolean quickMenuVisible;

    private AdvancedVideoQualityMenu() {}

    public static void markQuickMenuVisible() {
        quickMenuVisible = true;
    }

    public static void onFlyoutMenuCreate(View view) {
        if (!Settings.getBoolean("advanced_video_quality_menu", true) || !(view instanceof ViewGroup)) return;
        final ViewGroup list = (ViewGroup) view;
        list.getViewTreeObserver().addOnDrawListener(new ViewTreeObserver.OnDrawListener() {
            @Override
            public void onDraw() {
                if (!quickMenuVisible || list.getChildCount() == 0) return;
                quickMenuVisible = false;
                try {
                    View first = list.getChildAt(0);
                    if (!(first instanceof ViewGroup) || ((ViewGroup) first).getChildCount() < 4) return;
                    View advanced = ((ViewGroup) first).getChildAt(3);
                    ViewGroup parent = parentAt(list, 3);
                    if (advanced == null || parent == null) return;
                    advanced.setSoundEffectsEnabled(false);
                    if (!advanced.performClick()) return;
                    parent.setVisibility(View.GONE);
                    Logger.debug(() -> "Advanced video quality menu opened");
                } catch (Exception exception) {
                    Logger.error(() -> "Advanced video quality menu failure: " + exception);
                }
            }
        });
    }

    public static void addVideoQualityListMenuListener(View view) {
        if (!Settings.getBoolean("advanced_video_quality_menu", true) || !(view instanceof ListView)) return;
        final ListView list = (ListView) view;
        list.setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener() {
            @Override
            public void onChildViewAdded(View parent, View child) {
                try {
                    if (list.indexOfChild(child) != 4) return;
                    list.setSoundEffectsEnabled(false);
                    if (!list.performItemClick(child, 4, list.getItemIdAtPosition(4))) return;
                    parent.setVisibility(View.GONE);
                    Logger.debug(() -> "Advanced Shorts quality menu opened");
                } catch (Exception exception) {
                    Logger.error(() -> "Advanced Shorts quality menu failure: " + exception);
                }
            }

            @Override
            public void onChildViewRemoved(View parent, View child) {}
        });
    }

    public static boolean forceAdvancedVideoQualityMenuCreation(boolean original) {
        return Settings.getBoolean("advanced_video_quality_menu", true) || original;
    }

    private static ViewGroup parentAt(View view, int depth) {
        View current = view;
        for (int i = 0; i < depth && current.getParent() instanceof View; i++) {
            current = (View) current.getParent();
        }
        return current.getParent() instanceof ViewGroup ? (ViewGroup) current.getParent() : null;
    }

}
