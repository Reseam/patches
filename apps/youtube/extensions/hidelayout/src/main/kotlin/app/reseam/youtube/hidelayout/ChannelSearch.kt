// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.hidelayout

import android.util.LruCache
import android.widget.EditText
import app.reseam.runtime.Export
import app.reseam.youtube.core.Endpoints
import app.reseam.youtube.core.InnerTube
import app.reseam.youtube.core.Proto
import app.reseam.youtube.litho.Filter
import app.reseam.youtube.litho.FilterGroup.ByteArrayFilterGroup
import app.reseam.youtube.litho.FilterGroup.StringFilterGroup
import app.reseam.youtube.litho.LithoComponent
import app.reseam.youtube.navigation.NavigationBar
import app.reseam.youtube.settings.ChannelSettings

private const val CHANNEL_ID_PREFIX = "UC"
private const val CACHED_HANDLES = 32

/**
 * Scopes YouTube's search to the channel it was opened from. The query starts with the channel's
 * handle, which makes YouTube rank that channel's videos first, and results from other channels
 * are hidden while it does.
 */
object ChannelSearch {
    private class Scope(val handle: String, val channel: ByteArrayFilterGroup)

    private val handles = LruCache<String, String>(CACHED_HANDLES)
    @Volatile private var channelOnScreen: String? = null
    @Volatile private var scope: Scope? = null

    /** Every browse page passes here when it is shown, including ones returned to. */
    @Export
    @JvmStatic
    fun pageShown(endpoint: ByteArray) {
        val browse = Proto.bytes(endpoint, Endpoints.BROWSE)
        val channelId =
            browse?.let { Proto.string(it, Endpoints.BROWSE_ID) }?.takeIf {
                it.startsWith(CHANNEL_ID_PREFIX)
            }
        channelOnScreen = channelId
        // Looked up while the page is read, so the handle is ready when search opens.
        if (channelId != null && handles[channelId] == null) {
            InnerTube.channelHandle(channelId) { handle -> handle?.let { handles.put(channelId, it) } }
        }
    }

    /** Replaces the search screen's initial setText. */
    @Export
    @JvmStatic
    fun setQuery(field: EditText, text: CharSequence?) {
        val channelId = channelOnScreen?.takeIf { ChannelSettings.searchInsideChannels.value }
        val handle = channelId?.let { handles[it] }
        scope = handle?.let { Scope(it, ByteArrayFilterGroup(null, channelId)) }
        if (handle == null || !text.isNullOrEmpty()) {
            field.setText(text)
            return
        }
        field.setText("$handle ")
        field.setSelection(field.length())
    }

    internal fun isOtherChannel(component: LithoComponent): Boolean {
        val current = scope ?: return false
        val searchedHandle = NavigationBar.searchQuery.trimStart().substringBefore(' ')
        if (!searchedHandle.equals(current.handle, ignoreCase = true)) return false
        return !current.channel.check(component.buffer).isFiltered
    }
}

/** Hides search results from other channels while the search is scoped to one. */
@Export
class ChannelSearchFilter : Filter() {
    init {
        addPathCallbacks(rootCards)
    }

    override fun isFiltered(component: LithoComponent, group: StringFilterGroup): Boolean =
        super.isFiltered(component, group) &&
            component.isCardRoot &&
            NavigationBar.isSearchBarActive &&
            ChannelSearch.isOtherChannel(component)
}

// A card's parts match too but carry only their own data; the root carries the whole card.
private val LithoComponent.isCardRoot: Boolean
    get() = '.' !in path.removePrefix(identifier)
