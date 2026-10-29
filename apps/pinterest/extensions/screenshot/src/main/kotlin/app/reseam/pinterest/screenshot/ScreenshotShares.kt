// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("ScreenshotShares")

package app.reseam.pinterest.screenshot

import android.app.Activity
import app.reseam.pinterest.settings.ShareSettings
import app.reseam.runtime.Export
import java.util.concurrent.Executor

@Export
fun registerScreenCaptureCallback(activity: Activity, executor: Executor, callback: Activity.ScreenCaptureCallback) {
    if (!ShareSettings.hideScreenshotShare.value) activity.registerScreenCaptureCallback(executor, callback)
}
