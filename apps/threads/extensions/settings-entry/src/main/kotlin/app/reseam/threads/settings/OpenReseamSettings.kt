// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.threads.settings

import app.reseam.bundle.ReseamPatches
import app.reseam.runtime.Export

@Export
class OpenReseamSettings : () -> Any? {
    // Compose ignores what an onClick returns.
    override fun invoke(): Any? {
        ReseamPatches.bundle().openSettings()
        return null
    }
}
