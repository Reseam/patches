// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.x.core

import app.reseam.patch.CompatiblePackage
import app.reseam.patch.ExtClass
import app.reseam.patch.Type
import app.reseam.patch.invoke

const val X_PACKAGE = "com.twitter.android"

// Pinned: X changes what R8 keeps between releases (12.29 dropped the parameter-name strings older
// targets were anchored on), so targets are read against one release at a time.
val X: CompatiblePackage = X_PACKAGE("12.29.1-prod.01")

object XSettingsEntry : ExtClass("app.reseam.x.settings.XSettingsEntry") {
    val init by static(Type.Context)
}

object OpenReseamSettings : ExtClass("app.reseam.x.settings.OpenReseamSettings") {
    val invoke by method(returns = "kotlin.Unit")
}

object SettingsRows : ExtClass("app.reseam.x.settings.SettingsRows") {
    val section by static(returns = Type.Object)
    val single by static(Type.Object, returns = Type.List)
    val withSection by static(Type.List, Type.Object, returns = Type.List)
}

object FeatureSwitchOverrides : ExtClass("app.reseam.x.featureswitches.FeatureSwitchOverrides") {
    val lookup by static(Type.String, returns = Type.Object)
    val disable by static(Type.String, Type.String, Type.Boolean)
}

object TimelineQueryFilter : ExtClass("app.reseam.x.timeline.TimelineQueryFilter") {
    val hidePromoted by static(Type.String, Type.Boolean)
    val hideRecommendations by static(Type.String, Type.Boolean)
    val rewrite by static(Type.String, returns = Type.String)
}
