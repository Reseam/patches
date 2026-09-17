// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.misc;

import android.app.Activity;

import app.reseam.youtube.core.Logger;

public final class FixBackToExitGesture {
    private static boolean isTopView;

    private FixBackToExitGesture() {}

    public static void onBackPressed(Activity activity) {
        if (!isTopView) return;

        Logger.debug(() -> "Activity is closed");
        activity.finish();
    }

    public static void onScrollPositionRestored(int position) {
        // Zero/negative positions take the normal scroll-to-top path.
        if (position <= 0) return;
        Logger.debug(() -> "Views are scrolling");
        isTopView = false;
    }

    public static void onTopView() {
        Logger.debug(() -> "Scrolling reached the top");
        isTopView = true;
    }
}
