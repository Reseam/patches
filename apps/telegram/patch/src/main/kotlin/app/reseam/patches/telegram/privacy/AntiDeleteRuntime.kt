// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.telegram.privacy

import app.reseam.patch.appEntry
import app.reseam.patch.before
import app.reseam.patch.patch
import app.reseam.patches.telegram.core.DeletedArchive
import app.reseam.patches.telegram.core.TELEGRAM

/** Starts the deleted-message archive with the app; shared by every patch that calls into it. */
val antiDeleteRuntime = patch {
    compatibleWith(TELEGRAM)

    execute {
        appEntry.before {
            call(DeletedArchive.init, thisObject)
        }
    }
}
