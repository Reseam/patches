// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.misc;

import android.app.AlertDialog;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import java.util.Arrays;
import java.util.List;

import app.reseam.youtube.core.Logger;

/**
 * The player shows the same dialog class for several prompts, so the last playability status is
 * what says whether the dialog on screen is the age-restriction warning.
 */
public final class ViewerDiscretionDialog {
    private static final List<String> VIEWER_DISCRETION_STATUSES = Arrays.asList(
            "AGE_CHECK_REQUIRED",
            "AGE_VERIFICATION_REQUIRED",
            "CONTENT_CHECK_REQUIRED",
            "LOGIN_REQUIRED");

    private static volatile String playabilityStatus = "";

    private ViewerDiscretionDialog() {}

    /** Injection point, from the player's playability check; the patch gates the call on the setting. */
    public static void setPlayabilityStatus(Enum<?> status) {
        playabilityStatus = status == null ? "" : status.name();
    }

    /** Injection point, once the dialog exists; the patch gates the call on the setting. */
    public static void confirm(AlertDialog dialog) {
        if (!VIEWER_DISCRETION_STATUSES.contains(playabilityStatus)) return;

        if (!dialog.isShowing()) dialog.show();

        Button confirm = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (confirm == null) return;

        Window window = dialog.getWindow();
        if (window != null) {
            // Clicking alone leaves the dialog on screen for about a second while it animates out.
            WindowManager.LayoutParams attributes = window.getAttributes();
            attributes.height = 0;
            attributes.width = 0;
            window.setAttributes(attributes);
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }

        Logger.debug(() -> "Confirming viewer discretion dialog for " + playabilityStatus);
        confirm.callOnClick();
    }

    /**
     * Injection point, where the player picks a dialog style; the patch gates the call on the
     * setting. The modern style is drawn by an obfuscated class with no reachable buttons, so the
     * plain one is forced for the dialog this patch dismisses.
     */
    public static boolean useModernDialog(boolean original) {
        return original && !VIEWER_DISCRETION_STATUSES.contains(playabilityStatus);
    }
}
