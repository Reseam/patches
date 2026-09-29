// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.x.notifications

import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.section
import app.reseam.patch.settings.skipWhen
import app.reseam.patches.x.core.X
import app.reseam.patches.x.core.XSettings
import app.reseam.patches.x.core.xSettings
import app.reseam.patches.x.featureswitches.disableFeatureSwitches
import app.reseam.patches.x.featureswitches.featureSwitchOverrides

private const val PROMPT_SWITCH = "x_lite_notifications_permission_prompt_enabled"

val hideNotificationPrompts = patch("Hide notification prompts") {
    description("Stops the full-screen prompt, the dialog, and the banner asking to turn on notifications.")
    compatibleWith(X)
    dependsOn(featureSwitchOverrides)
    settings(xSettings, section("Notifications", XSettings.hideNotificationPrompts))

    execute {
        disableFeatureSwitches(
            XSettings.hideNotificationPrompts,
            PROMPT_SWITCH,
            "x_lite_notifications_permission_fullscreen_prompt_enabled",
            "x_lite_notifications_permission_banner_enabled",
        )
        // With its own prompt switched off, X asks Android for the permission at startup instead.
        startupTasks.point { string(PROMPT_SWITCH); then(within = 8) { invokeStatic { hasParam("kotlin.coroutines.CoroutineContext") } } }
            .skipWhen(XSettings.hideNotificationPrompts)
    }
}

private val startupTasks = method("startupTasks") { strings(PROMPT_SWITCH, "pending_token_checkin") }
