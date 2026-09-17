// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.misc

import app.reseam.patch.ExtClass
import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettings
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings

/**
 * ReVanced's "Enable debugging" is mostly a settings screen; the half that is not is this, the
 * hooks that let a user pin an A/B experiment flag on or off. The long and double accessors are
 * not hooked: nothing in the YouTube tree reads a numeric flag a user would want to change.
 */
val featureFlagOverrides = patch("Feature flag overrides") {
    description("Lets you pin YouTube's A/B experiment flags on or off by id.")
    compatibleWith(YOUTUBE)
    settings(youTubeSettings, section(YouTubeSettingsPages.Advanced, "Experiments", YouTubeSettings.featureFlagOverrides))

    execute {
        val accessors = klass(protoFeatureFlagParser.owner)
        methods("booleanFeatureFlag") {
            inClass(accessors)
            param(0, Type.Long)
            returns(Type.Boolean)
        }.forEach {
            after { returnValue(call(FeatureFlags.booleanOverride, param(0), capture("result"))) }
        }
        methods("stringFeatureFlag") {
            inClass(accessors)
            param(0, Type.Long)
            returns(Type.String)
        }.forEach {
            after { returnValue(call(FeatureFlags.stringOverride, param(0), capture("result"))) }
        }
    }
}

// The proto reader identifies the shared accessor class through its parse diagnostic.
val protoFeatureFlagParser = method("protoFeatureFlagParser") {
    strings("Unable to parse proto typed experiment flag: ")
    params(Type.Long, "[B")
}

object FeatureFlags : ExtClass("app.reseam.youtube.misc.FeatureFlags") {
    val booleanOverride = static("booleanOverride", Type.Long, Type.Boolean, returns = Type.Boolean)
    val stringOverride = static("stringOverride", Type.Long, Type.String, returns = Type.String)
}
