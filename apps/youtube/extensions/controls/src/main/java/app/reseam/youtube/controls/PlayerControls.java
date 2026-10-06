// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.controls;

import android.view.View;
import android.view.ViewTreeObserver;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import app.reseam.youtube.core.Logger;

/** Shared runtime state for controls grafted into YouTube's bottom player container. */
public final class PlayerControls {
    private static final Set<PlayerControlButton> BUTTONS =
            Collections.newSetFromMap(new WeakHashMap<>());
    private static volatile boolean shown;
    private static final Set<Runnable> VISIBILITY_LISTENERS = new java.util.concurrent.CopyOnWriteArraySet<>();
    private static WeakReference<View> fullscreenButton = new WeakReference<>(null);

    private PlayerControls() {}

    public static boolean isVisible() {
        return shown;
    }

    /** Register process-lifetime observers; callbacks must not capture an Activity. */
    public static void addVisibilityListener(Runnable listener) {
        VISIBILITY_LISTENERS.add(listener);
    }

    public static void register(PlayerControlButton button) {
        BUTTONS.add(button);
        button.setVisibility(shown, false);
    }

    public static void setVisibility(boolean visible, boolean animated) {
        boolean changed = shown != visible;
        shown = visible;
        for (PlayerControlButton button : BUTTONS.toArray(new PlayerControlButton[0])) {
            button.setVisibility(visible, animated);
        }
        Logger.debug(() -> "Player controls visibility: " + (visible ? "shown" : "hidden"));
        if (changed) for (Runnable listener : VISIBILITY_LISTENERS) listener.run();
    }

    public static void setVisibilityImmediate(boolean visible) {
        setVisibility(visible, false);
    }

    public static void setPlayerControlsVisibility(Enum<?> state) {
        if (state == null) return;
        final String name = state.name();
        if (name.endsWith("SHOWN") || name.endsWith("WILL_SHOW")) {
            setVisibility(true, true);
        } else if (name.endsWith("HIDDEN") || name.endsWith("WILL_HIDE")) {
            setVisibility(false, true);
        }
    }

    public static void setFullscreenCloseButton(View button) {
        fullscreenButton = new WeakReference<>(button);
        Logger.debug(() -> "Fullscreen button set");
        button.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            private int lastVisibility = button.getVisibility();
            private final int[] position = new int[2];
            private final int[] referencePosition = new int[2];
            private float alignmentOffset;
            private float appliedTranslation;

            @Override
            public void onGlobalLayout() {
                try {
                    // YouTube can replace the translation during a transition. An external
                    // value belongs entirely to YouTube; do not subtract our previous offset.
                    if (button.getTranslationY() != appliedTranslation) alignmentOffset = 0;
                    alignmentOffset = alignFullscreenButton(button, position, referencePosition, alignmentOffset);
                    appliedTranslation = button.getTranslationY();
                    final int visibility = button.getVisibility();
                    if (visibility != lastVisibility) {
                        lastVisibility = visibility;
                        final String name = visibility == View.VISIBLE
                                ? "VISIBLE" : visibility == View.GONE ? "GONE" : "INVISIBLE";
                        Logger.debug(() -> "fullscreen button visibility: " + name);
                        setVisibility(visibility == View.VISIBLE, false);
                    }
                } catch (Exception exception) {
                    Logger.error(() -> "Fullscreen button listener failure: " + exception);
                }
            }
        });
    }

    private static float alignFullscreenButton(View button, int[] position, int[] referencePosition,
                                              float alignmentOffset) {
        if (!button.isShown() || button.getHeight() == 0) return alignmentOffset;
        button.getLocationInWindow(position);
        float center = position[1] + button.getPaddingTop()
                + (button.getHeight() - button.getPaddingTop() - button.getPaddingBottom()) / 2f;
        View nearest = null;
        int distance = Integer.MAX_VALUE;
        for (PlayerControlButton control : BUTTONS) {
            View candidate = control.alignmentView();
            if (candidate == null || !candidate.isShown() || candidate.getHeight() == 0
                    || candidate.getRootView() != button.getRootView()) continue;
            candidate.getLocationInWindow(referencePosition);
            int dx = Math.abs(referencePosition[0] - position[0]);
            if (dx < distance) {
                nearest = candidate;
                distance = dx;
            }
        }
        if (nearest == null) {
            if (alignmentOffset != 0) button.setTranslationY(button.getTranslationY() - alignmentOffset);
            return 0;
        }
        nearest.getLocationInWindow(referencePosition);
        float target = referencePosition[1] + nearest.getPaddingTop()
                + (nearest.getHeight() - nearest.getPaddingTop() - nearest.getPaddingBottom()) / 2f;
        // Native portrait/fullscreen layouts use different sizes and padding. Move only
        // the view to the adjacent icon's baseline; leave its appearance and hit area intact.
        float delta = target - center;
        if (Math.abs(delta) >= 1) {
            button.setTranslationY(button.getTranslationY() + delta);
            alignmentOffset += delta;
        }
        return alignmentOffset;
    }
}
