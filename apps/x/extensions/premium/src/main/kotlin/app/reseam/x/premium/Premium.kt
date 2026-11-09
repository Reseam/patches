// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("Premium")

package app.reseam.x.premium

import app.reseam.runtime.Export

// X's servers enforce Premium+ and per-feature entitlements.
private const val PREMIUM = "feature/twitter_blue_verified"

private val tiers = setOf("feature/premium_basic", PREMIUM, "feature/premium_plus")

@Volatile private var subscribed = false

@Export
fun withPremium(stored: Set<String>?): Set<String> {
    subscribed = stored.orEmpty().any { it in tiers }
    return stored.orEmpty() + PREMIUM
}

/** Whether the account's own entitlements, before Premium is added, include a tier. */
@Export fun subscribed(): Boolean = subscribed
