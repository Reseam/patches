// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("FeedElements")

package app.reseam.reddit.feed

import app.reseam.runtime.Export
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

private val classSets = ConcurrentHashMap<String, Set<String>>()
private val comma = Pattern.compile(",")

// R8 renames the element classes, so the patch passes their names in, comma separated.
@Export
fun isAny(element: Any?, classNames: String): Boolean =
    element != null &&
        element.javaClass.name in
            classSets.computeIfAbsent(classNames) { comma.split(it).toHashSet() }

@Export
fun without(elements: List<Any?>, classNames: String): MutableList<Any?> =
    elements.filterTo(ArrayList(elements.size)) { !isAny(it, classNames) }
