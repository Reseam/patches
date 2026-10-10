// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.reddit.core

import app.reseam.patch.CompatiblePackage
import app.reseam.patch.ExtClass
import app.reseam.patch.Type
import app.reseam.patch.invoke

const val REDDIT_PACKAGE = "com.reddit.frontpage"

// Pinned: newer versions change code the targets were not read against.
val REDDIT: CompatiblePackage = REDDIT_PACKAGE("2026.39.0")

object RedditSettingsEntry : ExtClass("app.reseam.reddit.settings.RedditSettingsEntry") {
    val init by static(Type.Context)
}

object OpenReseamSettings : ExtClass("app.reseam.reddit.settings.OpenReseamSettings") {
    val invoke by method(returns = "kotlin.Unit")
}

object ReseamSettingsItem : ExtClass("app.reseam.reddit.settings.ReseamSettingsItem") {
    val sessions by static(returns = "java.util.Set")
    val withItem by static("java.util.Set", returns = "java.util.Set")
}

object ReseamSettingsIcon : ExtClass("app.reseam.reddit.settings.ReseamSettingsIcon") {
    val draw by static(Type.Object)
    val unit by static(returns = "kotlin.Unit")
}

object FeedElements : ExtClass("app.reseam.reddit.feed.FeedElements") {
    val isAny by static(Type.Object, Type.String, returns = Type.Boolean)
    val without by static(Type.List, Type.String, returns = Type.List)
}

object SettingsRows : ExtClass("app.reseam.reddit.settings.SettingsRows") {
    val withoutUpsells by static("java.util.Set", returns = "java.util.Set")
    val section by static(Type.Object, returns = "com.reddit.settings.usersettings.UserSettingsSection")
    val key by static(Type.Object, returns = Type.String)
}

object FlagOverrides : ExtClass("app.reseam.reddit.flags.FlagOverrides") {
    val isDisabled by static(Type.String, returns = Type.Boolean)
    val isForced by static(Type.String, returns = Type.Boolean)
    val spec by static(returns = Type.String)
}

object DynamicConfigOverrides : ExtClass("app.reseam.reddit.flags.DynamicConfigOverrides") {
    val intValue by static(Type.String, Type.String, returns = "java.lang.Integer")
    val stringValue by static(Type.String, Type.String, returns = Type.String)
}
