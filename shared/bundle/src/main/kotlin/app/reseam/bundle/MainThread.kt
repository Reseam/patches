// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.bundle

import android.os.Handler
import android.os.Looper

/** A fresh handler, so its owner can remove only its own callbacks. */
fun mainThreadHandler(): Handler = Handler(Looper.getMainLooper())

fun postToMainThread(action: () -> Unit): Boolean = mainThreadHandler().post(action)

fun isMainThread(): Boolean = Looper.myLooper() == Looper.getMainLooper()
