// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.x.featureswitches

import app.reseam.patch.PatchRuntime
import app.reseam.patch.Type
import app.reseam.patch.appEntry
import app.reseam.patch.before
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.ToggleSetting
import app.reseam.patches.x.core.FeatureSwitchOverrides
import app.reseam.patches.x.core.X

val featureSwitchOverrides = patch {
    compatibleWith(X)

    execute {
        // Every typed getter funnels through this one lookup.
        featureSwitchValue.before {
            val override = call(FeatureSwitchOverrides.lookup, param(0))
            whenNotNull(override) { returnValue(override) }
        }
    }
}

/** Forces the given feature switches off while [toggle] is on. Requires [featureSwitchOverrides]. */
fun PatchRuntime.disableFeatureSwitches(toggle: ToggleSetting, vararg keys: String) {
    appEntry.before {
        for (key in keys) call(FeatureSwitchOverrides.disable, string(key), string(toggle.key), bool(toggle.default))
    }
}

val featureSwitchValue = klass("com.x.featureswitches.FeatureSwitchesRepositoryImpl").method("getFeatureSwitchValue") {
    params(Type.String, Type.Boolean)
    returns(Type.Object)
}
