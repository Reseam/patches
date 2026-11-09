// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("AutoClaim")

package app.reseam.twitch.chat

import app.reseam.runtime.Export

private var lastClaimId: String? = null

@Export
@Synchronized
fun isFirstTry(claimId: String?): Boolean {
    if (claimId == null || claimId == lastClaimId) return false
    lastClaimId = claimId
    return true
}
