// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("ReplySort")

package app.reseam.x.postdetail

import app.reseam.runtime.Export
import app.reseam.x.settings.XSettings
import java.util.Locale

@Export
fun initial(sort: Enum<*>): Enum<*> {
    val chosen = XSettings.defaultReplySort.value
    return sort.declaringJavaClass.enumConstants?.firstOrNull { it.name.equals(chosen, ignoreCase = true) } ?: sort
}

@Export
fun remember(sort: Enum<*>) {
    XSettings.defaultReplySort.value = sort.name.lowercase(Locale.ROOT)
}
