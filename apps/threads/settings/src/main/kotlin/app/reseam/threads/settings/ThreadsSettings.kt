// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.threads.settings

import app.reseam.patch.settings.SettingsGroup
import app.reseam.patch.settings.toggle
import app.reseam.patches.bundle.lucide

object AdSettings : SettingsGroup(ThreadsStrings) {
    val hideAds by toggle(default = true, icon = lucide("megaphone-off"))
}
