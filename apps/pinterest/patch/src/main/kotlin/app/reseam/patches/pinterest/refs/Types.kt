// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.pinterest.refs

import app.reseam.patch.PatchRuntime

fun PatchRuntime.holdsField(owner: String, type: String) =
    bytecode.findClass(owner)!!.instanceFields.any { it.fieldType == type }
