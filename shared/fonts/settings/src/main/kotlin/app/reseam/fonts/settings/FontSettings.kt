// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.fonts.settings

import app.reseam.patch.settings.Choice
import app.reseam.patch.settings.SettingsGroup
import app.reseam.patch.settings.Vector
import app.reseam.patch.settings.action
import app.reseam.patch.settings.choice

object FontSettings : SettingsGroup(FontStrings) {
    val font by
        choice(
            default = "app",
            choices = listOf(
                Choice("app", FontStrings.fontApp),
                Choice("system", FontStrings.fontSystem),
                Choice("custom", FontStrings.fontCustom),
            ),
            icon = Vector.lucide("type"),
        )
    val fontFile by action(icon = Vector.lucide("file-type"))
}
