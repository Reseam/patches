// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.shorts

import app.reseam.runtime.Export
import app.reseam.youtube.litho.Filter

@Export
class ShortsFilter : Filter() {
    private val groups = ShortsFilterGroups()

    init {
        addIdentifierCallbacks(*groups.identifierGroups)
        addPathCallbacks(*groups.pathGroups)
    }
}
