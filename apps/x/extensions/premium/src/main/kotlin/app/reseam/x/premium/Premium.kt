// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("Premium")

package app.reseam.x.premium

import app.reseam.runtime.Export

// X's servers enforce Premium+ and per-feature entitlements.
private const val PREMIUM = "feature/twitter_blue_verified"

@Export fun withPremium(stored: Set<String>?): Set<String> = stored.orEmpty() + PREMIUM
