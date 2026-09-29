// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.reddit.core

import app.reseam.patch.ExtClass
import app.reseam.patch.Type

// Unpinned: a target that stops matching fails loudly, so a pin would only hide breakage.
const val REDDIT = "com.reddit.frontpage"

object RedditSettingsEntry : ExtClass("app.reseam.reddit.settings.RedditSettingsEntry") {
    val init = static("init", Type.Context)
    val open = static("open")
}

object OpenReseamSettings : ExtClass("app.reseam.reddit.settings.OpenReseamSettings") {
    val invoke = method("invoke", returns = "kotlin.Unit")
}

object ReseamSettingsItem : ExtClass("app.reseam.reddit.settings.ReseamSettingsItem") {
    val sessions = static("sessions", returns = "java.util.Set")
    val withItem = static("withItem", "java.util.Set", returns = "java.util.Set")
}

object ReseamSettingsIcon : ExtClass("app.reseam.reddit.settings.ReseamSettingsIcon") {
    val draw = static("draw", Type.Object)
    val unit = static("unit", returns = "kotlin.Unit")
}

object FeedElements : ExtClass("app.reseam.reddit.feed.FeedElements") {
    val isAny = static("isAny", Type.Object, Type.String, returns = Type.Boolean)
    val without = static("without", Type.List, Type.String, returns = Type.List)
}

object SettingsRows : ExtClass("app.reseam.reddit.settings.SettingsRows") {
    val withoutUpsells = static("withoutUpsells", "java.util.Set", returns = "java.util.Set")
    val section = static("section", Type.Object, returns = "com.reddit.settings.usersettings.UserSettingsSection")
    val key = static("key", Type.Object, returns = Type.String)
}

object FlagOverrides : ExtClass("app.reseam.reddit.flags.FlagOverrides") {
    val isDisabled = static("isDisabled", Type.String, returns = Type.Boolean)
    val isForced = static("isForced", Type.String, returns = Type.Boolean)
    val spec = static("spec", returns = Type.String)
}

object DynamicConfigOverrides : ExtClass("app.reseam.reddit.flags.DynamicConfigOverrides") {
    val intValue = static("intValue", Type.String, Type.String, returns = "java.lang.Integer")
    val stringValue = static("stringValue", Type.String, Type.String, returns = Type.String)
}
