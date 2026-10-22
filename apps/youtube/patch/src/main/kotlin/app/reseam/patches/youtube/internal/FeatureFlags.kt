// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.internal

import app.reseam.patch.ExtMethod
import app.reseam.patch.PointTarget
import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.methods
import app.reseam.patch.points

/** All reads of a YouTube boolean experiment, including inlined wrappers. */
internal fun booleanFeatureReads(flag: Long): List<PointTarget> =
    optionalBooleanFeatureReads(flag).also { check(it.isNotEmpty()) { "No boolean reads of feature $flag" } }

/** Like [booleanFeatureReads], for an experiment older releases do not have. */
internal fun optionalBooleanFeatureReads(flag: Long): List<PointTarget> =
    methods("feature $flag") { literals(flag) }.points {
        invokeVirtual { hasParam(Type.Long); returns(Type.Boolean) }
        argument(1) { literal(flag) }
    }.all

private fun booleanFeatureResults(reads: List<PointTarget>) =
    reads.map { it.next { resultOf(Type.Boolean) }.captureAs("enabled", Type.Boolean) }

internal fun overrideBooleanFeature(flag: Long, hook: ExtMethod) {
    booleanFeatureResults(booleanFeatureReads(flag)).forEach { it.after { capture("enabled").assign(call(hook, capture("enabled"))) } }
}

internal fun enableBooleanFeature(flag: Long) {
    booleanFeatureResults(optionalBooleanFeatureReads(flag)).forEach { it.after { capture("enabled").assign(bool(true)) } }
}
