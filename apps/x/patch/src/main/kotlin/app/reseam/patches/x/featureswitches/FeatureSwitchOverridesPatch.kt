// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.x.featureswitches

import app.reseam.patch.PatchRuntime
import app.reseam.patch.Type
import app.reseam.patch.appEntry
import app.reseam.patch.before
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.ToggleSetting
import app.reseam.patch.settings.section
import app.reseam.patches.x.core.X
import app.reseam.patches.x.core.xSettings
import app.reseam.x.featureswitches.bindings.FeatureSwitchOverrides
import app.reseam.x.settings.XSettings
import app.reseam.x.settings.XStrings

val featureSwitchOverrides = patch {
    compatibleWith(X)

    execute {
        // Every typed getter funnels through this one lookup.
        featureSwitchValue.before {
            val override = FeatureSwitchOverrides.lookup(param(0))
            whenNotNull(override) { returnValue(override) }
        }
    }
}

val userFeatureSwitches = patch("Feature switch overrides") {
    description("Sets X's feature switches to values you choose.")
    compatibleWith(X)
    dependsOn(featureSwitchOverrides)
    settings(xSettings, section(XStrings.sectionAdvanced, XSettings.featureSwitchOverrides))

    execute {
        appEntry { FeatureSwitchOverrides.applyUserOverrides() }
    }
}

/** Forces the given feature switches off while [toggle] is on. Requires [featureSwitchOverrides]. */
fun PatchRuntime.disableFeatureSwitches(toggle: ToggleSetting, vararg keys: String) {
    appEntry {
        for (key in keys) FeatureSwitchOverrides.disable(string(key), string(toggle.key), bool(toggle.default))
    }
}

val featureSwitchValue = klass("com.x.featureswitches.FeatureSwitchesRepositoryImpl").method("getFeatureSwitchValue") {
    params(Type.String, Type.Boolean)
    returns(Type.Object)
}
