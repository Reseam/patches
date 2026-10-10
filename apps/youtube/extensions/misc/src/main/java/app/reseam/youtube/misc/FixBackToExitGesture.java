// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.misc;

import android.app.Activity;

import app.reseam.youtube.core.Logger;

public final class FixBackToExitGesture {
    // Set only when the back press being handled scrolled a feed to the top. A scroll to the top
    // from anything else, such as reselecting a tab, must not close the app on a later back press.
    private static boolean isTopView;

    private FixBackToExitGesture() {}

    public static void onBackPressStarted() {
        isTopView = false;
    }

    public static void onBackPressed(Activity activity) {
        if (!isTopView) return;
        isTopView = false;

        Logger.debug(() -> "Activity is closed");
        activity.finish();
    }

    public static void onTopView() {
        Logger.debug(() -> "Scrolling reached the top");
        isTopView = true;
    }
}
