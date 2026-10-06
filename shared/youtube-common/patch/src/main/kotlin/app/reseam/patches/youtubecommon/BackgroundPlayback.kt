// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtubecommon

import app.reseam.patch.Type
import app.reseam.patch.method

// Reads the backgroundability extension (field 64657230) of the playability status.
val playableInBackground = method("playableInBackground") {
    literals(64657230L)
    returns(Type.Boolean)
    paramCount(1)
}
