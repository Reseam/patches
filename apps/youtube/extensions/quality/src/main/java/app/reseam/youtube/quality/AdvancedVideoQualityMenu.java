// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.quality;

import android.view.Choreographer;
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
                View first = list.getChildAt(0);
                if (!(first instanceof ViewGroup) || ((ViewGroup) first).getChildCount() < 4) return;
                View advanced = ((ViewGroup) first).getChildAt(3);
                advanced.setSoundEffectsEnabled(false);
                clickWhenSettled(list, advanced);
            }
        });
    }

    /**
     * The resolution list replaces the quick options inside the same sheet. Swapping them while the
     * sheet still slides in leaves the sheet sized for the old content with no rows, so the click
     * waits until the list holds its on-screen position for two frames.
     */
    private static void clickWhenSettled(View list, View advanced) {
        Choreographer.getInstance().postFrameCallback(new Choreographer.FrameCallback() {
            private final int[] location = new int[2];
            private int lastY = Integer.MIN_VALUE;
            private int stableFrames;

            @Override
            public void doFrame(long frameTimeNanos) {
                if (!advanced.isAttachedToWindow()) return;
                list.getLocationOnScreen(location);
                stableFrames = location[1] == lastY ? stableFrames + 1 : 0;
                lastY = location[1];
                if (stableFrames < 2) {
                    Choreographer.getInstance().postFrameCallback(this);
                } else if (advanced.performClick()) {
                    Logger.debug(() -> "Advanced video quality menu opened");
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
}
