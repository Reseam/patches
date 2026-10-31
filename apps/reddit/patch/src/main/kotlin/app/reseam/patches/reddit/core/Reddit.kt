// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.reddit.core

import app.reseam.patch.CompatiblePackage
import app.reseam.patch.invoke

const val REDDIT_PACKAGE = "com.reddit.frontpage"

// Pinned: newer versions change code the targets were not read against.
val REDDIT: CompatiblePackage = REDDIT_PACKAGE("2026.39.0", "2026.41.0")
