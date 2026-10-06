// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtubemusic.layout;

import static app.reseam.youtubemusic.core.MusicSettings.toggle;

import android.content.Context;
import android.media.AudioManager;
import android.view.KeyEvent;
import android.view.View;

/** The previous and next buttons the patch adds beside the miniplayer's play button. */
public final class MiniplayerButtons {
    private MiniplayerButtons() {}

    /** Injection point, with the miniplayer's play button once the miniplayer looks it up. */
    public static void bind(View playButton) {
        View row = (View) playButton.getParent();
        bind(row, "reseam_mini_player_previous", "miniplayer_previous_button", KeyEvent.KEYCODE_MEDIA_PREVIOUS);
        bind(row, "reseam_mini_player_next", "miniplayer_next_button", KeyEvent.KEYCODE_MEDIA_NEXT);
    }

    private static void bind(View row, String id, String setting, int key) {
        Context context = row.getContext();
        View button = row.findViewById(context.getResources().getIdentifier(id, "id", context.getPackageName()));
        if (button == null) return;
        button.setVisibility(toggle(setting, true).getAsBoolean() ? View.VISIBLE : View.GONE);
        // The app's own media session handles these keys, as it does for headset buttons.
        button.setOnClickListener(v -> {
            AudioManager audio = v.getContext().getSystemService(AudioManager.class);
            audio.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, key));
            audio.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, key));
        });
    }
}
