// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.pangram

import app.reseam.pangram.bindings.Pangram
import app.reseam.patch.PatchRuntime
import app.reseam.patch.appEntry

/** Adds Pangram as an AI labels detector. */
fun PatchRuntime.pangramSupport() {
    appEntry { Pangram.init(application) }
}
