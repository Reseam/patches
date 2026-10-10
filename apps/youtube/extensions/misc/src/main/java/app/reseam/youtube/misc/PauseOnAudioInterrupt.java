// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.misc;

import android.media.AudioManager;

import app.reseam.youtube.core.Logger;

public final class PauseOnAudioInterrupt {
    // A duck reaches YouTube again before it regains focus only when it asked for focus itself,
    // which it does when the user resumes playback. An app that keeps asking to duck would
    // otherwise pause every resume, so after the first pause the duck is left alone.
    private static boolean pausedForDuck;

    private PauseOnAudioInterrupt() {}

    public static int onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                if (pausedForDuck) {
                    Logger.debug(() -> "Resumed during an audio interrupt, lowering the volume instead");
                    return focusChange;
                }
                pausedForDuck = true;
                return AudioManager.AUDIOFOCUS_LOSS_TRANSIENT;
            case AudioManager.AUDIOFOCUS_GAIN:
            case AudioManager.AUDIOFOCUS_LOSS:
                pausedForDuck = false;
                return focusChange;
            default:
                return focusChange;
        }
    }
}
