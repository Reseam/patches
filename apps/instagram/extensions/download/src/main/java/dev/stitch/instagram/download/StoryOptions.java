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

final class StoryOptions {
    private static final String LABEL = "Download";

    private StoryOptions() {}

    static boolean isDownload(CharSequence option) {
        return option != null && LABEL.contentEquals(option);
    }

    static CharSequence[] appendDownload(CharSequence[] items) {
        if (items == null) return new CharSequence[]{LABEL};
        for (CharSequence item : items) {
            if (isDownload(item)) return items;
        }

        CharSequence[] expanded = new CharSequence[items.length + 1];
        System.arraycopy(items, 0, expanded, 0, items.length);
        expanded[items.length] = LABEL;
        return expanded;
    }
}
