// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.core;

import android.view.View;
import android.view.ViewGroup;

public final class Views {
    private Views() {}

    /** Hides a view and collapses its size, for parents that lay out gone children anyway. */
    public static void hide(View view) {
        if (view == null) return;
        view.setVisibility(View.GONE);
        ViewGroup.LayoutParams params = view.getLayoutParams();
        // Preserve the parent's LayoutParams subtype; a generic replacement can fail on attachment.
        if (params != null && (params.width != 0 || params.height != 0)) {
            params.width = 0;
            params.height = 0;
            view.setLayoutParams(params);
        }
    }
}
