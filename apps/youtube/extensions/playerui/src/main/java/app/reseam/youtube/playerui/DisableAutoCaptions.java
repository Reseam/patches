// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.playerui;

import app.reseam.youtube.core.Logger;
import app.reseam.youtube.core.Settings;

public final class DisableAutoCaptions {
    private static volatile boolean captionsButtonStatus;

    private DisableAutoCaptions() {}

    public static boolean disableAutoCaptions() {
        final boolean disable = Settings.getBoolean("disable_auto_captions", false) && !captionsButtonStatus;
        Logger.debug(() -> "DisableAutoCaptions: " + disable + " status=" + captionsButtonStatus);
        return disable;
    }

    public static void setCaptionsButtonStatus(boolean status) {
        captionsButtonStatus = status;
        Logger.debug(() -> "DisableAutoCaptions.status: " + status);
    }
}
