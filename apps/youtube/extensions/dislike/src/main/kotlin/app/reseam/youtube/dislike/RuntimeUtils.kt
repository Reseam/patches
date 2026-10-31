// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.dislike

import android.view.View
import app.reseam.bundle.isMainThread
import app.reseam.bundle.isNightMode
import app.reseam.youtube.core.YouTubeContext
import java.util.concurrent.Executors

internal val dislikeFetchExecutor =
    Executors.newFixedThreadPool(3) { task ->
        Thread(task, "Reseam-RYD").apply { isDaemon = true }
    }

internal fun containsNumber(text: CharSequence): Boolean = text.any(Char::isDigit)

internal val isRightToLeftLocale: Boolean
    get() =
        YouTubeContext.applicationContext.resources.configuration.layoutDirection ==
            View.LAYOUT_DIRECTION_RTL

internal val textDirectionString: String
    get() = if (isRightToLeftLocale) "\u200f" else "\u200e"

internal val isDarkModeEnabled: Boolean
    get() = YouTubeContext.applicationContext.resources.isNightMode()

internal fun verifyOffMainThread() = check(!isMainThread()) { "Network call on UI thread" }

internal fun verifyOnMainThread() = check(isMainThread()) { "UI operation off main thread" }
