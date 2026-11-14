// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.ailabels

import app.reseam.ailabels.bindings.AiLabels
import app.reseam.ailabels.settings.AiLabelsSettings
import app.reseam.patch.PatchRuntime
import app.reseam.patch.appEntry
import app.reseam.patch.settings.onAction

val aiLabelSettings = listOf(
    AiLabelsSettings.aiLabels,
    AiLabelsSettings.aiLabelImages,
    AiLabelsSettings.aiDetector,
    AiLabelsSettings.aiDetectorAccount,
)

/** The app-independent half of an app's AI labels patch. Each detector's own support adds itself. */
fun PatchRuntime.aiLabelsSupport() {
    appEntry { AiLabels.init(application) }
    onAction(AiLabelsSettings.aiDetectorAccount) { AiLabels.openAccount(activity) }
}
