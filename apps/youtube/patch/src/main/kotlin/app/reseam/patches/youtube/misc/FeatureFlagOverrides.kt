// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.misc

import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.klass
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.youtube.misc.bindings.FeatureFlags
import app.reseam.youtube.settings.AdvancedSettings
import app.reseam.youtube.settings.YouTubeStrings

/**
 * ReVanced's "Enable debugging" is mostly a settings screen; the half that is not is this, the
 * hooks that let a user pin an A/B experiment flag on or off. The long and double accessors are
 * not hooked: nothing in the YouTube tree reads a numeric flag a user would want to change.
 */
val featureFlagOverrides = patch("Feature flag overrides") {
    description("Lets you pin YouTube's A/B experiment flags on or off by id.")
    compatibleWith(YOUTUBE)
    settings(youTubeSettings, section(YouTubeSettingsPages.Advanced, YouTubeStrings.sectionExperiments, AdvancedSettings.featureFlagOverrides))

    execute {
        val accessors = klass(protoFeatureFlagParsers.all.map { it.owner }.distinct().single())
        methods("booleanFeatureFlag") {
            inClass(accessors)
            hasParam(Type.Long)
            custom { parameterTypes.first() == Type.Long || parameterTypes.drop(1) == listOf(Type.Long, Type.Boolean) }
            returns(Type.Boolean)
        }.forEach {
            // Older releases pass the flag store before the id to static accessors.
            val id = parameterTypes.indexOf(Type.Long)
            after { returnValue(FeatureFlags.booleanOverride(param(id), result)) }
        }
        methods("stringFeatureFlag") {
            inClass(accessors)
            hasParam(Type.Long)
            custom { parameterTypes.first() == Type.Long || parameterTypes.drop(1) == listOf(Type.Long, Type.String) }
            returns(Type.String)
        }.forEach {
            val id = parameterTypes.indexOf(Type.Long)
            after { returnValue(FeatureFlags.stringOverride(param(id), result)) }
        }
    }
}

// The proto readers identify one shared accessor class; older releases have several specialisations.
private val protoFeatureFlagParsers = methods("protoFeatureFlagParsers") {
    strings("Unable to parse proto typed experiment flag: ")
    hasParam(Type.Long)
    hasParam("[B")
}
