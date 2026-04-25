// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later


package app.reseam.instagram.download;

import android.content.Context;
import android.view.View;

import java.util.List;

public final class MediaMeta {
    private MediaMeta() {}

    // All bodies are rewritten at patch time to read app objects directly
    // through the download patch bindings and resolved member refs.

    public static String username(Object media) {
        return null;
    }

    public static String videoUrl(Object media) {
        return null;
    }

    public static String imageUrl(Object media) {
        return null;
    }

    public static List carouselChildren(Object media) {
        return null;
    }

    public static Object listenerMedia(Object listener) {
        return null;
    }

    public static Object listenerDownloadOption(Object listener) {
        return null;
    }

    public static Object reelItemMedia(Object reelItem) {
        return null;
    }

    public static Object storyOwnerReelItem(Object owner) {
        return null;
    }

    public static Object storyOwnerContext(Object owner) {
        return null;
    }

    public static void addLegacyMenuRow(
            Object menu,
            Context context,
            View.OnClickListener listener,
            String label,
            int icon,
            boolean extra) {
        // Patched at runtime to invoke-virtual the resolved addRow method on
        // the legacy reels menu class.
    }
}
