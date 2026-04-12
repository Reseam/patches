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

import android.util.Log;

final class Logcat {
    private static final String TAG = "Stitch";

    private Logcat() {}

    static void d(String message) {
        Log.d(TAG, message);
    }

    static void w(String message) {
        Log.w(TAG, message);
    }

    static void e(String message) {
        Log.e(TAG, message);
    }

    static void e(String message, Throwable throwable) {
        Log.e(TAG, message + ": " + throwable, throwable);
    }
}
