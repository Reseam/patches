// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.misc;

import android.view.Display;

import app.reseam.youtube.core.Settings;

public final class VideoCodecs {
    private VideoCodecs() {}

    /** Injection point: every {@code getSupportedHdrTypes} call in the app comes here instead. */
    public static int[] supportedHdrTypes(Display.HdrCapabilities capabilities) {
        return Settings.getBoolean("disable_hdr_video", false)
                ? new int[0]
                : capabilities.getSupportedHdrTypes();
    }
}
