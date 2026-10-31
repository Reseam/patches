// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

@file:JvmName("FixBackToExitGesture")

package app.reseam.youtube.misc

import android.app.Activity
import app.reseam.bundle.Logger
import app.reseam.runtime.Export
import app.reseam.youtube.core.debug

// Only reaching the top during this back press should close the activity.
private var isTopView = false

@Export
fun onBackPressStarted() {
    isTopView = false
}

@Export
fun onBackPressed(activity: Activity) {
    if (!isTopView) return
    isTopView = false
    activity.finish()
}

@Export
fun onTopView() {
    Logger.debug { "Scrolling reached the top" }
    isTopView = true
}
