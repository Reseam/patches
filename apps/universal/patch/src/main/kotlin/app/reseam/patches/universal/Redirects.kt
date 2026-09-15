// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.universal

import app.reseam.patch.BytecodeScope
import app.reseam.patch.ExtMethod
import app.reseam.patch.dex.parameterTypes

/** Redirects calls to each method on the type of its first parameter, the receiver, to that extension method. */
internal fun BytecodeScope.redirectInstanceCalls(vararg methods: ExtMethod): Int =
    methods.sumOf { redirectCalls(it.ref.parameterTypes.first(), it.name, it) }
