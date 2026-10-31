// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.core

import android.util.Log
import app.reseam.bundle.Logger
import app.reseam.youtubecommon.settings.YouTubeCommonSettings

fun Logger.debug(error: Throwable? = null, message: () -> String) {
    if (YouTubeCommonSettings.debugLogging.value) Log.d(Logger.TAG, message(), error)
}
