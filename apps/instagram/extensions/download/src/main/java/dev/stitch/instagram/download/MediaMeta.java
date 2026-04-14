/*
 * Stitch — part of the Stitch Android patcher.
 * Copyright (C) 2026 Aunali321 <accounts@auna.li>
 *
 * This file is part of Stitch.
 *
 * Stitch is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Stitch is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package dev.stitch.instagram.download;

import android.content.Context;
import android.view.View;

public final class MediaMeta {
    private MediaMeta() {}

    // All bodies are replaced at patch time with direct invokes using
    // fingerprint-resolved method/field refs. See download patch UrlHooks /
    // OwnerHooks / MenuHooks in the patch module.

    public static Object feedHandlerMedia(Object handler) {
        return null;
    }

    public static Object reelItemMedia(Object reelItem) {
        return null;
    }

    public static Object storyOwnerReelItem(Object owner) {
        return null;
    }

    public static Object storyOwnerContext(Object owner) {
        return null;
    }

    public static void addLegacyMenuRow(
            Object menu,
            Context context,
            View.OnClickListener listener,
            String label,
            int icon,
            boolean extra) {
        // Patched at runtime to invoke-virtual the resolved addRow method on
        // the legacy reels menu class.
    }
}
