// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

@file:JvmName("LayoutViews")

package app.reseam.youtube.hidelayout

import android.view.View
import android.widget.TextView
import app.reseam.runtime.Export
import app.reseam.runtime.TextValue
import app.reseam.runtime.ToggleValue
import app.reseam.youtube.core.hide
import app.reseam.youtube.settings.ChannelSettings
import app.reseam.youtube.settings.FeedSettings

/** Implemented on the app's search suggestions by the patch. */
@Export
interface SearchSuggestionAccessor {
    @Export fun patch_isSearchHistory(): Boolean
}

@Export fun hideAlbumCard(view: View?) = collapse(view, FeedSettings.hideAlbumCards)

@Export fun hideCrowdfundingBox(view: View?) = collapse(view, FeedSettings.hideCrowdfundingBox)

@Export
fun hideSubscribedChannelsBar(view: View?) = collapse(view, FeedSettings.hideSubscribedChannelsBar)

@Export
fun hideLatestVideosButton(view: View?) = gone(view, FeedSettings.hideLatestVideosButton.value)

@Export
fun hideInRelatedVideos(view: View?) =
    gone(view, FeedSettings.hideFilterBarFeedInRelatedVideos.value)

@Export
fun hideShowMoreButton(root: View?, buttonContainer: View?, textView: TextView?) {
    if (!FeedSettings.hideShowMoreButton.value || root == null || textView == null) return
    if (root.contentDescription?.toString() != textView.text?.toString()) return
    gone(root)
    gone(buttonContainer)
}

@Export(name = "hideFlyoutMenu")
fun hideFlyoutMenu(title: CharSequence?): CharSequence? = title.takeUnless {
    isHiddenFlyoutItem(it)
}

@Export(name = "hideFlyoutMenuForTablet")
fun hideFlyoutMenu(textView: TextView?, title: CharSequence?) {
    if (isHiddenFlyoutItem(title)) gone(textView?.parent as? View)
}

@Export
fun hideChannelTabView(tabView: View?, tabText: String?) {
    if (
        ChannelSettings.hideChannelTab.value &&
            tabText in ChannelSettings.hideChannelTabFilterStrings
    )
        gone(tabView)
}

@Export
fun hideYouMayLikeSection(typedString: String?): Boolean =
    FeedSettings.hideYouMayLikeSection.value && typedString.isNullOrEmpty()

@Export
fun isSearchHistory(searchTerm: Any?, endpoint: String?): Boolean =
    endpoint?.contains("/delete") == true

/** The app's list can be immutable, so a new one is returned. */
@Export
fun filterSearchSuggestions(suggestions: Collection<*>): Collection<Any?> =
    suggestions.filterTo(ArrayList()) {
        it !is SearchSuggestionAccessor || it.patch_isSearchHistory()
    }

private fun isHiddenFlyoutItem(title: CharSequence?): Boolean =
    title != null &&
        FeedSettings.hideFeedFlyoutMenu.value &&
        title in FeedSettings.hideFeedFlyoutMenuFilterStrings

private operator fun TextValue.contains(text: CharSequence?): Boolean =
    text != null &&
        value.lineSequence().map(String::trim).any {
            it.isNotEmpty() && it.contentEquals(text, ignoreCase = true)
        }

private fun collapse(view: View?, setting: ToggleValue) {
    if (setting.value) hide(view)
}

private fun gone(view: View?, hidden: Boolean = true) {
    if (hidden) view?.visibility = View.GONE
}
