// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.x.notifications

import app.reseam.patch.Type
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.x.XPatchStrings
import app.reseam.patches.x.core.X
import app.reseam.patches.x.core.XSettingsPages
import app.reseam.x.settings.XSettings
import app.reseam.patches.x.core.xSettings
import app.reseam.patches.x.featureswitches.disableFeatureSwitches
import app.reseam.patches.x.featureswitches.featureSwitchOverrides
import app.reseam.x.settings.XStrings

private const val PROMPT_SWITCH = "x_lite_notifications_permission_prompt_enabled"

val hideNotificationPrompts = patch(XPatchStrings.hideNotificationPrompts) {
    description(XPatchStrings.hideNotificationPromptsDescription)
    compatibleWith(X)
    dependsOn(featureSwitchOverrides)
    settings(xSettings, section(XSettingsPages.Ads, XStrings.sectionNotifications, XSettings.hideNotificationPrompts))

    execute {
        disableFeatureSwitches(
            XSettings.hideNotificationPrompts,
            PROMPT_SWITCH,
            "x_lite_notifications_permission_fullscreen_prompt_enabled",
            "x_lite_notifications_permission_banner_enabled",
        )
        // With its own prompt switched off, X asks Android for the permission at startup instead,
        // so this one read keeps reporting the switch as on.
        gate(XSettings.hideNotificationPrompts) {
            startupTasks.point { string(PROMPT_SWITCH) }
                .next { resultOf(Type.Boolean) }
                .captureAs("promptEnabled", Type.Boolean)
                .after { capture("promptEnabled").assign(bool(true)) }
        }
    }
}

private val startupTasks = method("startupTasks") { strings(PROMPT_SWITCH, "pending_token_checkin") }
