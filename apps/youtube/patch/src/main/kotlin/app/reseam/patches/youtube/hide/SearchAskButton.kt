// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.hide

import app.reseam.patches.youtube.internal.overrideOptionalBooleanFeature
import app.reseam.youtube.hidelayout.bindings.LayoutViews

// Adds Ask YouTube to the search box (id/ai_mode) and its hint; 20.26 has neither.
private const val SEARCH_ASK = 45763762L

internal fun hideSearchAskButton() = overrideOptionalBooleanFeature(SEARCH_ASK, LayoutViews.searchAskEnabled)
