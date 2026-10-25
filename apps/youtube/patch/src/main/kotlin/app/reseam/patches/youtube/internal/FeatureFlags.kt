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
    optionalBooleanFeatureReads(flag).also { check(it.isNotEmpty()) { "Feature $flag is missing" } }

/**
 * Like [booleanFeatureReads], for an experiment that only some supported releases have: empty when the
 * release never mentions [flag], and a failure when it does but none of its reads match.
 */
internal fun optionalBooleanFeatureReads(flag: Long): List<PointTarget> {
    val users = methods("feature $flag") { literals(flag) }
    if (users.all.isEmpty()) return emptyList()
    return users.points {
        invokeVirtual { hasParam(Type.Long); returns(Type.Boolean) }
        argument(1) { literal(flag) }
    }.all.also { check(it.isNotEmpty()) { "No boolean reads of feature $flag" } }
}

private fun booleanFeatureResults(reads: List<PointTarget>) =
    reads.map { it.next { resultOf(Type.Boolean) }.captureAs("enabled", Type.Boolean) }

internal fun overrideBooleanFeature(flag: Long, hook: ExtMethod) {
    booleanFeatureResults(booleanFeatureReads(flag)).forEach { it.after { capture("enabled").assign(call(hook, capture("enabled"))) } }
}

/** Turns on [flag] wherever the release reads it. */
internal fun enableBooleanFeature(flag: Long) = forceBooleanFeature(flag, true)

/** Turns off [flag] wherever the release reads it. */
internal fun disableBooleanFeature(flag: Long) = forceBooleanFeature(flag, false)

private fun forceBooleanFeature(flag: Long, enabled: Boolean) {
    booleanFeatureResults(optionalBooleanFeatureReads(flag)).forEach { it.after { capture("enabled").assign(bool(enabled)) } }
}
