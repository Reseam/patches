// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.player;

import android.view.View;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import app.reseam.youtube.core.Logger;

/**
 * Whether the Shorts player is on screen. Tracked from the Shorts player view itself, so it stays
 * right where {@link PlayerType} alone is ambiguous.
 */
public final class ShortsPlayerState {
    private static volatile boolean open;
    private static final Set<View> observed = Collections.newSetFromMap(new WeakHashMap<>());
    private static final Set<View> attached = Collections.newSetFromMap(new WeakHashMap<>());

    private ShortsPlayerState() {}

    /** Injection point, with the inflated Shorts player view. */
    public static void attach(View shortsPlayer) {
        if (shortsPlayer == null || !observed.add(shortsPlayer)) return;
        if (shortsPlayer.isAttachedToWindow()) {
            attached.add(shortsPlayer);
            setOpen(true);
        }
        shortsPlayer.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View view) {
                attached.add(view);
                setOpen(true);
            }

            @Override
            public void onViewDetachedFromWindow(View view) {
                attached.remove(view);
                setOpen(!attached.isEmpty());
            }
        });
    }

    public static boolean isOpen() {
        return open;
    }

    private static void setOpen(boolean value) {
        if (open == value) return;

        open = value;
        Logger.debug(() -> "Shorts player open: " + value);
    }
}
