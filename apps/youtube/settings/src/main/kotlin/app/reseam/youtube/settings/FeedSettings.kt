// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.settings

import app.reseam.patch.settings.SettingsGroup
import app.reseam.patch.settings.text
import app.reseam.patch.settings.toggle

object FeedSettings : SettingsGroup(YouTubeStrings, YOUTUBE_KEYS) {
    val hideAlbumCards by toggle(default = false)
    val hideArtistCards by toggle(default = false)
    val hideChipsShelf by toggle(default = true)
    val hideCommunityPosts by toggle(default = false)
    val hideCompactBanner by toggle(default = true)
    val hideCreatorStoreShelf by toggle(default = true)
    val hideCrowdfundingBox by toggle(default = false)
    val hideDoodles by toggle(default = false)
    val hideEmergencyBox by toggle(default = true)
    val hideExpandableCard by toggle(default = true)
    val hideFeedFlyoutMenu by toggle(default = false)
    val hideFeedFlyoutMenuFilterStrings by text(default = "", multiline = true)
    val hideFilterBarFeedInFeed by toggle(default = false)
    val hideFilterBarFeedInHistory by toggle(default = false)
    val hideFilterBarFeedInRelatedVideos by toggle(default = false)
    val hideFilterBarFeedInSearch by toggle(default = false)
    val hideFloatingMicrophoneButton by toggle(default = true)
    val hideForYouShelf by toggle(default = false)
    val hideGetPremiumButton by toggle(default = true)
    val hideHorizontalShelves by toggle(default = true)
    val hideImageShelf by toggle(default = true)
    val hideLatestPosts by toggle(default = true)
    val hideLatestVideosButton by toggle(default = false)
    val hideLiveChatReplayButton by toggle(default = false)
    val hideMedicalPanels by toggle(default = true)
    val hideMixPlaylists by toggle(default = true)
    val hideMoviesSection by toggle(default = true)
    val hidePlayables by toggle(default = true)
    val hideShowMoreButton by toggle(default = true)
    val hideSubscribedChannelsBar by toggle(default = false)
    val hideSurveys by toggle(default = true)
    val hideTicketShelf by toggle(default = false)
    val hideTimedReactions by toggle(default = true)
    val hideVideoRecommendationLabels by toggle(default = true)
    val hideViewCount by toggle(default = false)
    val hideVisualSpacer by toggle(default = true)
    val hideWebSearchResults by toggle(default = true)
    val hideYouMayLikeSection by toggle(default = true)
    val liveRingsOpenChannel by toggle(default = false)
    val removeFeedScrollSpeedLimit by toggle(default = true)
}
