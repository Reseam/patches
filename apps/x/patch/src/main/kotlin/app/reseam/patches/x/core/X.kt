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
    val init = static("init", Type.Context)
    val open = static("open")
}

object OpenReseamSettings : ExtClass("app.reseam.x.settings.OpenReseamSettings") {
    val invoke = method("invoke", returns = "kotlin.Unit")
}

object SettingsRows : ExtClass("app.reseam.x.settings.SettingsRows") {
    val section = static("section", returns = Type.Object)
    val single = static("single", Type.Object, returns = Type.List)
    val withSection = static("withSection", Type.List, Type.Object, returns = Type.List)
}

object FeatureSwitchOverrides : ExtClass("app.reseam.x.featureswitches.FeatureSwitchOverrides") {
    val lookup = static("lookup", Type.String, returns = Type.Object)
    val disable = static("disable", Type.String, Type.String, Type.Boolean)
}

object TimelineQueryFilter : ExtClass("app.reseam.x.timeline.TimelineQueryFilter") {
    val hidePromoted = static("hidePromoted", Type.String, Type.Boolean)
    val hideRecommendations = static("hideRecommendations", Type.String, Type.Boolean)
    val rewrite = static("rewrite", Type.String, returns = Type.String)
}
