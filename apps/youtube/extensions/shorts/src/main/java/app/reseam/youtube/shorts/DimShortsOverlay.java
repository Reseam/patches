// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.shorts;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Build;
import android.view.View;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowInsetsController;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import app.reseam.youtube.core.Logger;
import app.reseam.youtube.core.Settings;
import app.reseam.youtube.player.ShortsPlayerState;

/** Dims the Shorts overlay and toolbar while the Shorts player is attached. */
public final class DimShortsOverlay {
    private static int reelTimeBarId;
    private static final Set<View> dimmedViews = Collections.newSetFromMap(new WeakHashMap<>());

    private DimShortsOverlay() {}

    public static void dimShortsPlayerOverlay(View shortsOverlay) {
        if (shortsOverlay == null) return;
        Logger.debug(() -> "DimShorts overlay hook fired");
        View.OnAttachStateChangeListener attachment = new View.OnAttachStateChangeListener() {
            private ViewTreeObserver observer;
            private ViewTreeObserver.OnPreDrawListener preDraw;
            private Window window;
            private boolean statusBarHidden;
            private boolean timeBarHooked;

            @Override
            public void onViewAttachedToWindow(View view) {
                ViewParent parent = view.getParent();
                View target = parent instanceof View ? (View) parent : view;
                window = getWindow(target);
                if (reelTimeBarId == 0) {
                    reelTimeBarId = view.getResources().getIdentifier(
                            "reel_time_bar", "id", view.getContext().getPackageName());
                }
                preDraw = () -> {
                    target.setAlpha(opacity());
                    if (!timeBarHooked && window != null && reelTimeBarId != 0) {
                        View timeBar = window.getDecorView().findViewById(reelTimeBarId);
                        if (timeBar != null) {
                            addShortsAwareDimListener(timeBar);
                            timeBarHooked = true;
                        }
                    }
                    boolean immersive = Settings.getBoolean("dim_shorts_overlay_immersive_mode", true);
                    if (window != null && immersive != statusBarHidden) {
                        statusBarHidden = immersive;
                        setStatusBarHidden(window, immersive);
                    }
                    return true;
                };
                observer = target.getViewTreeObserver();
                observer.addOnPreDrawListener(preDraw);
            }

            @Override
            public void onViewDetachedFromWindow(View view) {
                if (observer != null && observer.isAlive() && preDraw != null) {
                    observer.removeOnPreDrawListener(preDraw);
                }
                if (window != null && statusBarHidden) setStatusBarHidden(window, false);
                observer = null;
                preDraw = null;
                window = null;
                statusBarHidden = false;
                timeBarHooked = false;
            }
        };
        shortsOverlay.addOnAttachStateChangeListener(attachment);
        if (shortsOverlay.isAttachedToWindow()) attachment.onViewAttachedToWindow(shortsOverlay);
    }

    public static void dimShortsToolbarButton(Enum<?> button, View view) {
        Logger.debug(() -> "DimShorts toolbar hook fired: "
                + (button == null ? "null" : button.name()));
        if (view != null) addShortsAwareDimListener(view);
    }

    private static float opacity() {
        try {
            int value = Integer.parseInt(Settings.getString("dim_shorts_overlay_opacity", "90"));
            return Math.max(0, Math.min(100, value)) / 100f;
        } catch (NumberFormatException exception) {
            return 1f;
        }
    }

    private static Window getWindow(View view) {
        Context context = view.getContext();
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) return ((Activity) context).getWindow();
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    private static void setStatusBarHidden(Window window, boolean hide) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = window.getInsetsController();
            if (controller == null) return;
            if (hide) {
                controller.hide(android.view.WindowInsets.Type.statusBars());
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            } else {
                controller.show(android.view.WindowInsets.Type.statusBars());
            }
            return;
        }
        View decor = window.getDecorView();
        int flags = decor.getSystemUiVisibility();
        if (hide) flags |= View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        else flags &= ~(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        decor.setSystemUiVisibility(flags);
    }

    private static void addShortsAwareDimListener(View view) {
        if (!dimmedViews.add(view)) return;
        ViewTreeObserver.OnPreDrawListener[] listener = new ViewTreeObserver.OnPreDrawListener[1];
        ViewTreeObserver[] observer = new ViewTreeObserver[1];
        View.OnAttachStateChangeListener attachListener = new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View attached) {
                listener[0] = () -> {
                    attached.setAlpha(ShortsPlayerState.isOpen() ? opacity() : 1f);
                    return true;
                };
                observer[0] = attached.getViewTreeObserver();
                observer[0].addOnPreDrawListener(listener[0]);
            }

            @Override
            public void onViewDetachedFromWindow(View detached) {
                if (observer[0] != null && observer[0].isAlive() && listener[0] != null) {
                    observer[0].removeOnPreDrawListener(listener[0]);
                }
                observer[0] = null;
                listener[0] = null;
            }
        };
        view.addOnAttachStateChangeListener(attachListener);
        if (view.isAttachedToWindow()) attachListener.onViewAttachedToWindow(view);
    }
}
