// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.x.core

import app.reseam.patch.ExtClass
import app.reseam.patch.Type

// Unpinned: a target that stops matching fails loudly, so a pin would only hide breakage.
const val X = "com.twitter.android"

object XSettingsEntry : ExtClass("app.reseam.x.settings.XSettingsEntry") {
    val init = static("init", Type.Context)
    val open = static("open")
}

object OpenReseamSettings : ExtClass("app.reseam.x.settings.OpenReseamSettings") {
    val invoke = method("invoke", returns = "kotlin.Unit")
}

object SettingsRows : ExtClass("app.reseam.x.settings.SettingsRows") {
    val single = static("single", Type.Object, returns = Type.List)
    val withSection = static("withSection", Type.List, Type.Object, returns = Type.List)
}

object FeatureSwitchOverrides : ExtClass("app.reseam.x.featureswitches.FeatureSwitchOverrides") {
    val lookup = static("lookup", Type.String, returns = Type.Object)
    val disable = static("disable", Type.String, Type.String, Type.Boolean)
}

object TimelineQueryFilter : ExtClass("app.reseam.x.ads.TimelineQueryFilter") {
    val init = static("init", Type.String, Type.Boolean)
    val rewrite = static("rewrite", Type.String, returns = Type.String)
}
