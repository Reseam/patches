// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.ailabels.settings

import app.reseam.patch.settings.Choice
import app.reseam.patch.settings.SettingsGroup
import app.reseam.patch.settings.TakesEffect
import app.reseam.patch.settings.Vector
import app.reseam.patch.settings.action
import app.reseam.patch.settings.choice
import app.reseam.patch.settings.toggle

object AiLabelsSettings : SettingsGroup(AiLabelsStrings) {
    val aiLabels by toggle(default = false, icon = Vector.lucide("scan-text"), takesEffect = TakesEffect.IMMEDIATELY)
    val aiLabelImages by toggle(default = false, icon = Vector.lucide("image"), takesEffect = TakesEffect.IMMEDIATELY)
    val aiDetector by
        choice(
            default = "pangram",
            choices = listOf(Choice("pangram", AiLabelsStrings.aiDetectorPangram)),
            icon = Vector.lucide("scan-search"),
            takesEffect = TakesEffect.IMMEDIATELY,
        )
    val aiDetectorAccount by action(icon = Vector.lucide("user-round"))
}
