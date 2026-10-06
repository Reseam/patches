// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.x.timeline

import app.reseam.patch.Type
import app.reseam.patch.before
import app.reseam.patch.method
import app.reseam.patch.methodTarget
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patches.x.core.TimelineQueryFilter
import app.reseam.patches.x.core.X

private const val TIMELINE_VIEW_SCHEMA_QUERY = "SELECT name, sql FROM sqlite_master WHERE type = 'view' AND name = 'TimelineView'"

// Timeline entries reach the UI only through SQL on timeline_entry; rewriting statements in every
// SQLiteConnection implementation survives Room regenerating the DAO text.
val timelineQueryFilter = patch {
    compatibleWith(X)

    execute {
        val prepare = connectionPrepare
        val implementations = bytecode.classes
            .filter { prepare.owner in it.interfaces }
            .mapNotNull { it.method(prepare.name, prepare.proto) }
            .filter { it.instructionCount > 0 }
        require(implementations.isNotEmpty()) { "no implementation of ${prepare.descriptor} found" }
        for (implementation in implementations) {
            methodTarget("prepare:${implementation.owner}") { implementation }.before {
                param(0).assign(call(TimelineQueryFilter.rewrite, param(0)))
            }
        }
    }
}

// Room validates the view on open with this fixed statement, calling SQLiteConnection.prepare.
val timelineViewSchemaCheck = method("timelineViewSchemaCheck") { strings(TIMELINE_VIEW_SCHEMA_QUERY) }

val connectionPrepare = timelineViewSchemaCheck
    .point { string(TIMELINE_VIEW_SCHEMA_QUERY) }
    .next { invokeInterface { params(Type.String) } }
    .callee("connectionPrepare")
