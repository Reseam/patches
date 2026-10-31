// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.hidelayout

import android.os.SystemClock
import app.reseam.bundle.Logger
import app.reseam.runtime.Export
import app.reseam.youtube.core.debug
import app.reseam.youtube.litho.ByteTrieSearch
import app.reseam.youtube.litho.Filter
import app.reseam.youtube.litho.FilterGroup.StringFilterGroup
import app.reseam.youtube.litho.FilterRule
import app.reseam.youtube.litho.LithoComponent
import app.reseam.youtube.litho.StringTrieSearch
import app.reseam.youtube.litho.matches
import app.reseam.youtube.navigation.NavigationBar
import app.reseam.youtube.player.PlayerType
import app.reseam.youtube.settings.FilterSettings
import java.util.concurrent.atomic.AtomicReference

private val nestedCards =
    FilterRule(
        null,
        "modern_type_shelf_header_content.e",
        "shorts_lockup_cell.e",
        "video_card.e",
    )

@Export
class KeywordContentFilter : Filter() {
    private val exceptions = StringTrieSearch<Unit>()
    @Volatile private var parsedText: String? = null
    @Volatile private var search = ByteTrieSearch<AtomicReference<String>>()
    @Volatile private var resumeAt = 0L
    @Volatile private var filteredPercentage = 0f

    init {
        arrayOf("metadata.e", "thumbnail.e", "avatar.e", "overflow_button.e").forEach {
            exceptions.addPattern(it)
        }
        addPathCallbacks(rootCards, nestedCards)
    }

    override fun isFiltered(component: LithoComponent, group: StringFilterGroup): Boolean {
        if (!super.isFiltered(component, group)) return false
        if (!isEnabledForCurrentSurface() || exceptions.matches(component.path)) return false
        parseIfNeeded()
        val matched = AtomicReference<String>()
        val filtered = search.matches(component.buffer, matched)
        if (filtered) Logger.debug { "KeywordContentFilter matched keyword ${matched.get()}" }
        updateBackoff(filtered)
        return filtered
    }

    private fun isEnabledForCurrentSurface(): Boolean {
        if (resumeAt != 0L) {
            if (SystemClock.elapsedRealtime() < resumeAt) return false
            resumeAt = 0
            filteredPercentage = 0f
            Logger.debug { "KeywordContentFilter resumed after broad-filter pause" }
        }
        if (PlayerType.current.isMaximizedOrFullscreen)
            return FilterSettings.hideKeywordContentHome.value
        if (NavigationBar.isSearchBarActive) return FilterSettings.hideKeywordContentSearch.value
        val hideHome = FilterSettings.hideKeywordContentHome.value
        val hideSubscriptions = FilterSettings.hideKeywordContentSubscriptions.value
        if (!hideHome && !hideSubscriptions) return false
        val selected = NavigationBar.NavigationButton.selectedNavigationButton ?: return hideHome
        return when (selected) {
            NavigationBar.NavigationButton.HOME,
            NavigationBar.NavigationButton.EXPLORE -> hideHome
            NavigationBar.NavigationButton.SUBSCRIPTIONS -> hideSubscriptions
            else -> false
        }
    }

    @Synchronized
    private fun parseIfNeeded() {
        val raw = FilterSettings.hideKeywordContentPhrases.value
        if (raw == parsedText) return
        val keywords = parseKeywords(raw)
        search =
            ByteTrieSearch<AtomicReference<String>>().apply {
                keywords.forEach { (keyword, match) ->
                    addPattern(keyword.toByteArray(Charsets.UTF_8)) { bytes, start, length, matched
                        ->
                        match.accepts(bytes, start, length).also { if (it) matched.set(keyword) }
                    }
                }
            }
        parsedText = raw
        resumeAt = 0
        filteredPercentage = 0f
        Logger.debug { "KeywordContentFilter loaded ${keywords.size} phrases" }
    }

    @Synchronized
    private fun updateBackoff(filtered: Boolean) {
        val updated = filteredPercentage * ((50f - 1) / 50f) + if (filtered) 1f / 50f else 0f
        if (updated > .95f) {
            resumeAt = SystemClock.elapsedRealtime() + 60_000L
            filteredPercentage = 0f
            Logger.debug { "KeywordContentFilter paused after filtering nearly every component" }
        } else {
            filteredPercentage = updated
        }
    }
}
