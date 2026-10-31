// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.bundle

import android.util.Log

object Logger {
    const val TAG = "Reseam"

    fun info(error: Throwable? = null, message: () -> String) {
        Log.i(TAG, message(), error)
    }

    fun warn(error: Throwable? = null, message: () -> String) {
        Log.w(TAG, message(), error)
    }

    fun error(error: Throwable? = null, message: () -> String) {
        Log.e(TAG, message(), error)
    }
}
