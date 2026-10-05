// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.x.home

import app.reseam.patch.dex.ref
import app.reseam.patch.fieldTarget
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.x.core.X
import app.reseam.patches.x.core.XSettings
import app.reseam.patches.x.core.xSettings
import app.reseam.patches.x.featureswitches.disableFeatureSwitches
import app.reseam.patches.x.featureswitches.featureSwitchOverrides

val defaultToFollowing = patch("Default to Following") {
    description("Opens Following by default and prevents automatic switches to For You.")
    compatibleWith(X)
    dependsOn(featureSwitchOverrides)
    settings(xSettings, section("Home", XSettings.defaultToFollowing))

    execute {
        disableFeatureSwitches(
            XSettings.defaultToFollowing,
            "android_home_back_open_for_you_on_app_restart_enabled",
            "android_home_back_to_for_you_enabled",
        )

        // Every path through the lookup, including its fallbacks to For You, returns the starting tab.
        gate(XSettings.defaultToFollowing) {
            initialHomeTab.after {
                returnValue(staticField(followingHomeTabInstance))
            }
        }
    }
}

val initialHomeTab = method("initialHomeTab") { strings("PreferredTabRepo") }
val followingHomeTab = klass("followingHomeTab") {
    strings("Following")
    extends(initialHomeTab.returnType)
}
val followingHomeTabInstance = fieldTarget("followingHomeTabInstance") {
    followingHomeTab.classDef.staticFields.single { it.fieldType == followingHomeTab.descriptor }.ref
}
