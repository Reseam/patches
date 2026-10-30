// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("FollowsYouIndicator")

package app.reseam.instagram.follows

import app.reseam.instagram.settings.FollowSettings
import app.reseam.instagram.settings.InstagramStrings
import app.reseam.runtime.Export

@Export
external fun appendFromSession(subtitle: String?, userSession: Any, userViewModel: Any): String?

@Export
fun maybeAppend(subtitle: String?, followedBy: Boolean?): String? {
    if (!FollowSettings.followsYouIndicator.value || followedBy != true) return subtitle
    val label = InstagramStrings.followsYouLabel
    val suffix = InstagramStrings.followsYouSuffix
    return when {
        subtitle.isNullOrEmpty() -> label
        subtitle.endsWith(suffix) || subtitle == label -> subtitle
        else -> InstagramStrings.followsYouSubtitle(subtitle)
    }
}
