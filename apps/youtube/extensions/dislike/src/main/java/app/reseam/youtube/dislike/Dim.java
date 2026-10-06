// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.dislike;

import app.reseam.youtube.core.YouTubeContext;

final class Dim {
    private Dim() {}
    static int dp(float value) {
        return Math.round(value * YouTubeContext.get().getResources().getDisplayMetrics().density);
    }
}
