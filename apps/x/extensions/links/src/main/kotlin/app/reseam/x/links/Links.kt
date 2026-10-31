// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("Links")

package app.reseam.x.links

import app.reseam.runtime.Export
import app.reseam.x.settings.XSettings
import java.util.concurrent.ConcurrentHashMap

private val expansions = ConcurrentHashMap<String, String>()

@Export
fun record(tco: String?, expanded: String?) {
    if (tco.isNullOrBlank() || expanded.isNullOrBlank() || tco == expanded) return
    if (expansions.size > 10_000) expansions.clear()
    expansions[tco] = expanded
}

@Export
fun canonical(url: String?): String? {
    if (url == null || !XSettings.canonicalLinks.value) return url
    return expansions[url] ?: url
}
