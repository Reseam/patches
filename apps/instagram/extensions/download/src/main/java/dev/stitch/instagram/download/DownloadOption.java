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

@SuppressWarnings({"rawtypes", "unchecked"})
final class DownloadOption {
    private static final String OPTION_CLASS = "com.instagram.feed.media.mediaoption.MediaOption$Option";

    private static Object download;
    private static boolean initialized;

    private DownloadOption() {}

    static boolean isDownload(Object option) {
        ensureInitialized();
        return download != null && download.equals(option);
    }

    private static void ensureInitialized() {
        if (initialized) return;

        try {
            Class optionClass = Class.forName(OPTION_CLASS);
            download = Enum.valueOf(optionClass, "DOWNLOAD");
        } catch (Throwable t) {
            Logcat.e("Failed to resolve DOWNLOAD enum", t);
        }
        initialized = true;
    }
}
