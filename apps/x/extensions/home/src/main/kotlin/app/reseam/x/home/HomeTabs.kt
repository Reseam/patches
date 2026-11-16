// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("HomeTabs")

package app.reseam.x.home

import app.reseam.runtime.Export
import app.reseam.x.settings.XSettings

@Export external fun isForYou(tab: Any): Boolean

@Export external fun isFollowing(tab: Any): Boolean

/** Home needs one of the two fixed tabs, so hiding For You wins over hiding Following. */
@Export
fun visible(tabs: Array<Any>): Array<Any> = when {
    XSettings.hideForYouTab.value -> tabs.filterNot(::isForYou).toTypedArray()
    XSettings.hideFollowingTab.value -> tabs.filterNot(::isFollowing).toTypedArray()
    else -> tabs
}
