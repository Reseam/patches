// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.threads.settings

import app.reseam.patch.settings.SettingsGroup
import app.reseam.patch.settings.Vector
import app.reseam.patch.settings.toggle

object AdSettings : SettingsGroup(ThreadsStrings) {
    val hideAds by toggle(default = true, icon = Vector.lucide("megaphone-off"))
}
