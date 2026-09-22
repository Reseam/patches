// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.dislike;

import app.reseam.youtube.core.YouTubeContext;

final class Dim {
    private Dim() {}
    static int dp(float value) {
        return Math.round(value * YouTubeContext.get().getResources().getDisplayMetrics().density);
    }
}
