// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.hidelayout

import app.reseam.youtube.litho.ByteTrieSearch
import app.reseam.youtube.litho.FilterGroup.ByteArrayFilterGroup
import app.reseam.youtube.litho.LithoComponent
import app.reseam.youtube.litho.StringTrieSearch
import app.reseam.youtube.litho.matches
import app.reseam.youtube.navigation.NavigationBar
import app.reseam.youtube.player.PlayerType
import app.reseam.youtube.settings.DescriptionSettings
import app.reseam.youtube.settings.FeedSettings
import java.util.concurrent.atomic.AtomicReference

internal class LayoutFiltering {
    private val exceptions =
        StringTrieSearch<Unit>().apply {
            arrayOf(
                    "home_video_with_context",
                    "related_video_with_context",
                    "search_video_with_context",
                    "comment_thread",
                    "|comment.",
                    "library_recent_shelf",
                )
                .forEach { addPattern(it) }
        }
    private val ticketShelf = ByteArrayFilterGroup(null, "ticket_item.e")
    private val playables = ByteArrayFilterGroup(null, "FEmini_app_destination")
    private val creatorStore = ByteArrayFilterGroup(null, "shopping_item_card_list")
    private val descriptionSearch =
        ByteTrieSearch<AtomicReference<Boolean>>().apply {
            mapOf(
                    "yt_fill_star" to DescriptionSettings.hideFeaturedPlacesSection,
                    "yt_fill_experimental_star" to DescriptionSettings.hideFeaturedPlacesSection,
                    "yt_outline_gaming" to DescriptionSettings.hideGamingSection,
                    "yt_outline_experimental_gaming" to DescriptionSettings.hideGamingSection,
                    "yt_outline_audio" to DescriptionSettings.hideMusicSection,
                    "yt_outline_experimental_audio" to DescriptionSettings.hideMusicSection,
                    "post_base_wrapper_slim" to DescriptionSettings.hideQuizzesSection,
                    "cell_video_attribute" to DescriptionSettings.hideAttributesSection,
                )
                .forEach { (pattern, setting) ->
                    addPattern(pattern.toByteArray(Charsets.UTF_8)) { _, _, _, matched ->
                        matched.set(setting.value)
                        true
                    }
                }
        }
    val groups = LayoutFilterGroups(this)

    fun allowsFeedPath(path: String): Boolean = !exceptions.matches(path)

    fun hideHorizontalShelf(content: LithoComponent): Boolean {
        if (content.index != 0) return false
        val buffer = content.buffer
        val descriptionResult = AtomicReference<Boolean>()
        if (descriptionSearch.matches(buffer, descriptionResult))
            return descriptionResult.get() == true
        val hideGeneric = FeedSettings.hideHorizontalShelves.value
        val hideTickets = FeedSettings.hideTicketShelf.value
        val hidePlayables = FeedSettings.hidePlayables.value
        val hideStore = FeedSettings.hideCreatorStoreShelf.value
        if (!hideGeneric && !hideTickets && !hidePlayables && !hideStore) return false
        return when {
            ticketShelf.check(buffer).isFiltered -> hideTickets
            playables.check(buffer).isFiltered -> hidePlayables
            creatorStore.check(buffer).isFiltered -> hideStore
            else -> {
                val watchingVideo = PlayerType.current.isMaximizedOrFullscreen
                (hideStore && watchingVideo) || (hideGeneric && hideShelves())
            }
        }
    }
}

private fun hideShelves(): Boolean =
    when {
        PlayerType.current.isMaximizedOrFullscreen -> false
        NavigationBar.isSearchBarActive -> true
        NavigationBar.isBackButtonVisible -> false
        else ->
            NavigationBar.NavigationButton.selectedNavigationButton !=
                NavigationBar.NavigationButton.LIBRARY
    }
