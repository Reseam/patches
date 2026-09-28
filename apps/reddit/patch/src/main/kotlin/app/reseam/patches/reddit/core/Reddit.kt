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

object FeedAds : ExtClass("app.reseam.reddit.ads.FeedAds") {
    val isAd = static("isAd", Type.Object, returns = Type.Boolean)
    val adClassNames = static("adClassNames", returns = Type.String)
}

object FlagOverrides : ExtClass("app.reseam.reddit.flags.FlagOverrides") {
    val isDisabled = static("isDisabled", Type.String, returns = Type.Boolean)
    val isForced = static("isForced", Type.String, returns = Type.Boolean)
    val spec = static("spec", returns = Type.String)
}
