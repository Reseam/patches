// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.tiktok.search

import app.reseam.patch.Type
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.tiktok.core.TIKTOK
import app.reseam.patches.tiktok.core.TikTokSettingsPages
import app.reseam.patches.tiktok.core.tiktokSettings
import app.reseam.tiktok.settings.InterfaceSettings
import app.reseam.tiktok.settings.TikTokStrings

private const val MIDDLE_PAGE = "com.ss.android.ugc.aweme.search.pages.middlepage"

val hideSearchSuggestions =
    patch("Hide search suggestions") {
        description("Removes the \"You may like\" suggestions from TikTok's search page.")
        compatibleWith(TIKTOK)
        settings(
            tiktokSettings,
            section(
                TikTokSettingsPages.SearchAndApp,
                TikTokStrings.sectionSearch,
                InterfaceSettings.hideSearchSuggestions,
            ),
        )

        execute {
            gate(InterfaceSettings.hideSearchSuggestions) {
                suggestionsRequest.alwaysReturn()
                searchPagePreload.alwaysReturn(false)
            }
        }
    }

private val suggestionsRequest =
    method("suggestionsRequest") {
        inClass(klass("$MIDDLE_PAGE.core.viewmodel.SuggestWordsViewModel"))
        calls {
            owner("com.ss.android.ugc.aweme.search.performance.metrics.SearchMiddleMonitor")
            name("recommendRequestStart")
        }
        returns(Type.Void)
    }

private val searchPagePreload =
    klass("$MIDDLE_PAGE.core.utils.IntermediatePreload").method("enable") { returns(Type.Boolean) }
