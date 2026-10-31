// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.telegram.privacy

import app.reseam.patch.appEntry
import app.reseam.patch.patch
import app.reseam.patches.telegram.core.TELEGRAM
import app.reseam.telegram.antidelete.bindings.ArchiveStorage

/** Starts the deleted-message archive with the app; shared by every patch that calls into it. */
val antiDeleteRuntime = patch {
    compatibleWith(TELEGRAM)

    execute {
        appEntry {
            ArchiveStorage.init(application)
        }
    }
}
