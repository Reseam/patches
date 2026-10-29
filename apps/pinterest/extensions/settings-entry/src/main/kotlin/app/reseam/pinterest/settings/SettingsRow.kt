// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("SettingsRow")

package app.reseam.pinterest.settings

import app.reseam.runtime.Export
import java.lang.ref.WeakReference

// The menu builds a new row each time it opens.
private var row = WeakReference<Any>(null)

@Export
fun remember(item: Any) {
    row = WeakReference(item)
}

@Export
fun isReseam(item: Any?): Boolean = item != null && item === row.get()
