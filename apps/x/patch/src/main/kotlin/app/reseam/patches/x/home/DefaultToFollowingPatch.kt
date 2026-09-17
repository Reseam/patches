// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.x.home

import app.reseam.patch.Type
import app.reseam.patch.dex.Opcode
import app.reseam.patch.dex.ref
import app.reseam.patch.fieldTarget
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.before
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

        // Hook the predicate constructor after the branch join, so every initial-tab path runs it.
        initialHomeTab.captureAs("tab").next {
            invokeDirect { name("<init>"); params(Type.Object, Type.Int) }
        }.before(XSettings.defaultToFollowing) {
            capture("tab").assign(staticField(followingHomeTabInstance))
        }
    }
}

val initialHomeTabs = method("initialHomeTabs") { strings("PreferredTabRepo") }
val initialHomeTab = initialHomeTabs.point { string("PreferredTabRepo") }.next { opcode(Opcode.SGET_OBJECT) }
val followingHomeTab = klass("followingHomeTab") {
    strings("Following")
    extends(klass(initialHomeTab.field().type).classDef.superclass ?: error("home tab superclass missing"))
}
val followingHomeTabInstance = fieldTarget("followingHomeTabInstance") {
    followingHomeTab.classDef.staticFields.single { it.fieldType == followingHomeTab.descriptor }.ref
}
