// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.internal

import app.reseam.patch.ExtMethod
import app.reseam.patch.PointTarget
import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.methods
import app.reseam.patch.points

/** All reads of a YouTube boolean experiment, including inlined wrappers. */
internal fun booleanFeatureReads(flag: Long): List<PointTarget> =
    methods("feature $flag") { literals(flag) }.points {
        invokeVirtual { hasParam(Type.Long); returns(Type.Boolean) }
        argument(1) { literal(flag) }
    }.all.also { check(it.isNotEmpty()) { "No boolean reads of feature $flag" } }

internal fun overrideBooleanFeature(flag: Long, hook: ExtMethod) {
    booleanFeatureReads(flag).forEach {
        it.next { resultOf(Type.Boolean) }.captureAs("enabled", Type.Boolean).after {
            capture("enabled").assign(call(hook, capture("enabled")))
        }
    }
}
