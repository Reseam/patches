// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("StorySuggestions")

package app.reseam.instagram.stories

import app.reseam.runtime.Export

// The server's reel_type names survive obfuscation; the enum's field names do not.
@Export
fun isSuggested(reelType: Enum<*>?): Boolean =
    when (reelType?.name) {
        "SUGGESTED_USER",
        "SUGGESTED_USER_REEL",
        "SUGGESTED_CREATOR_REEL" -> true
        else -> false
    }
