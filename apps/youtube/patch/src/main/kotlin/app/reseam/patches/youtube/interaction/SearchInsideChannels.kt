// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.interaction

import app.reseam.patch.Type
import app.reseam.patch.before
import app.reseam.patch.classTarget
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.redirectTo
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.patches.youtube.internal.navigationBarHook
import app.reseam.patches.youtube.internal.navigationEndpoint
import app.reseam.patches.youtubecommon.lithoFilter
import app.reseam.patches.youtubecommon.protobufBytes
import app.reseam.patches.youtubecommon.registerLithoFilter
import app.reseam.youtube.hidelayout.bindings.ChannelSearch
import app.reseam.youtube.hidelayout.bindings.ChannelSearchFilter
import app.reseam.youtube.settings.ChannelSettings
import app.reseam.youtube.settings.YouTubeStrings

val searchInsideChannels = patch("Search inside channels") {
    description("Starts searches made on a channel page with the channel's handle and shows only that channel's results.")
    compatibleWith(YOUTUBE)
    dependsOn(lithoFilter, navigationBarHook)
    settings(youTubeSettings, section(YouTubeSettingsPages.Channel, YouTubeStrings.sectionChannel, ChannelSettings.searchInsideChannels))

    execute {
        gate(ChannelSettings.searchInsideChannels) { browsePageShown.before { ChannelSearch.pageShown(protobufBytes(param(0))) } }
        searchFieldText.redirectTo(ChannelSearch.setQuery)
        registerLithoFilter(ChannelSearchFilter)
    }
}

private val browsePageView = method("browsePageView") {
    strings("Browse Fragment was given a navigation endpoint without browse data.")
    returns(Type.View)
}

private val browsePage = classTarget("browsePage") {
    bytecode.findClass(browsePageView.owner) ?: error("The browse page class is missing")
}

// The page is handed its endpoint each time it is shown, a page returned to with Back included.
private val browsePageShown = method("browsePageShown") {
    inClass(browsePage)
    params(navigationEndpoint.descriptor)
    returns(Type.Void)
}

// Opening search fills the field with the query it was opened for, empty for a new search.
private val searchFieldText = method("searchView") {
    strings("search_with_your_voice")
    returns(Type.View)
}.point("searchFieldText") {
    invokeVirtual { owner("android.widget.EditText"); name("setText"); params(Type.CharSequence) }
}
