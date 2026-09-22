// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
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

            @Override
            public void onGlobalLayout() {
                try {
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
}
