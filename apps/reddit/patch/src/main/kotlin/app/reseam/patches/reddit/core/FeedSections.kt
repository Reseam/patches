// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.reddit.core

import app.reseam.patch.Type
import app.reseam.patch.className
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.ToggleSetting

fun feedSectionContent(label: String) = method("$label content") {
    inClass(klass(label) { strings(label) })
    returns(Type.Void)
    paramCount(3)
    param(2, Type.Int)
}

val cellGroupMapper = method("cellGroupMapper") { strings("error parsing cell group ", "Required identifier of type ") }

fun feedElementClassNames(labels: List<String>) = labels.joinToString(",") { className(klass(it) { strings(it) }.descriptor) }

// The mapper already returns null for empty cell groups, so its callers skip null.
fun hideFeedElements(setting: ToggleSetting, labels: List<String>) {
    val classNames = feedElementClassNames(labels)
    gate(setting) {
        cellGroupMapper.after {
            whenTrue(call(FeedElements.isAny, capture("result"), string(classNames))) { returnNull() }
        }
    }
}
