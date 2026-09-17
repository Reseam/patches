// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.quality;

import app.reseam.youtube.core.Logger;
import app.reseam.youtube.core.Settings;

/** Chooses the original audio representation when YouTube offers dubbed tracks. */
public final class ForceOriginalAudio {
    private static volatile boolean enabled;

    private ForceOriginalAudio() {}

    public static void setEnabled() {
        enabled = Settings.getBoolean("force_original_audio", true);
        Logger.debug(() -> "Force original audio: " + enabled);
    }

    /** While enabled, only the original track (id suffix ".4") is default; a dubbed default is not. */
    public static boolean isDefaultAudioStream(boolean original, String audioTrackId,
                                               String audioTrackDisplayName) {
        // Older targets list empty placeholder tracks before the real ones.
        if (!enabled || audioTrackId == null || audioTrackId.isEmpty()) return original;
        final boolean originalTrack = audioTrackId.endsWith(".4");
        Logger.debug(() -> "Audio track: default=" + original + " id=" + audioTrackId
                + " name=" + audioTrackDisplayName);
        if (originalTrack) Logger.debug(() -> "Using original audio: " + audioTrackId);
        return originalTrack;
    }
}
