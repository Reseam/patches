// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("SettingsRows")

package app.reseam.reddit.settings

import app.reseam.runtime.Export
import com.reddit.settings.usersettings.UserSettingsSection

// App icons and the subscriber's own Premium row live in Premium but are not upsells.
private val kept = setOf("key_pref_alt_icons", "key_pref_premium")
private val upsells = setOf(UserSettingsSection.Premium, UserSettingsSection.RedditPro)

@Export
fun withoutUpsells(items: Set<Any>): MutableSet<Any> =
    items.filterTo(LinkedHashSet()) { item ->
        section(item) !in upsells || key(item) in kept
    }

// Reddit's settings item interface is R8-named, so the patch emits these calls.
@Export
external fun section(item: Any): UserSettingsSection

@Export external fun key(item: Any): String
