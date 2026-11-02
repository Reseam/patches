// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("FollowsYouIndicator")

package app.reseam.instagram.follows

import app.reseam.instagram.settings.FollowSettings
import app.reseam.instagram.settings.InstagramStrings
import app.reseam.runtime.Export
import java.util.Collections
import java.util.WeakHashMap

// Profile view model to the user it shows; the header binds from the view model's state, not the user.
private val profileUsers = Collections.synchronizedMap(WeakHashMap<Any, Any>())

@Export external fun followedBy(user: Any): Boolean?

private fun withFollowsYou(name: String?): String {
    val label = InstagramStrings.followsYouLabel
    return when {
        name.isNullOrEmpty() -> label
        name.endsWith(InstagramStrings.followsYouSuffix) || name == label -> name
        else -> InstagramStrings.followsYouSubtitle(name)
    }
}

@Export
fun rememberProfileUser(viewModel: Any, user: Any?) {
    if (user != null) profileUsers[viewModel] = user
}

@Export
fun profileName(name: String?, viewModel: Any?): String? {
    if (!FollowSettings.followsYouIndicator.value) return name
    val user = viewModel?.let(profileUsers::get) ?: return name
    return if (followedBy(user) == true) withFollowsYou(name) else name
}
