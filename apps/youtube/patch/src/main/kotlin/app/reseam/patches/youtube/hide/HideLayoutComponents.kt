// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.hide

import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.patches.youtube.internal.engagementPanelHook
import app.reseam.patches.youtubecommon.lithoFilter
import app.reseam.patches.youtubecommon.registerLithoFilter
import app.reseam.youtube.hidelayout.bindings.CommentsFilter
import app.reseam.youtube.hidelayout.bindings.DescriptionComponentsFilter
import app.reseam.youtube.hidelayout.bindings.KeywordContentFilter
import app.reseam.youtube.hidelayout.bindings.LayoutComponentsFilter
import app.reseam.youtube.settings.ChannelSettings
import app.reseam.youtube.settings.CommentsSettings
import app.reseam.youtube.settings.DescriptionSettings
import app.reseam.youtube.settings.FeedSettings
import app.reseam.youtube.settings.FilterSettings
import app.reseam.youtube.settings.PlayerSettings
import app.reseam.youtube.settings.YouTubeStrings
import app.reseam.youtubecommon.settings.YouTubeCommonSettings

val hideLayoutComponents = patch("Hide layout components") {
    description("Adds options to hide feed, player, description, comment and channel components.")
    compatibleWith(YOUTUBE)
    dependsOn(lithoFilter, engagementPanelHook, app.reseam.patches.youtube.internal.navigationBarHook)
    settings(
        youTubeSettings,
        section(
            YouTubeSettingsPages.Feed,
            YouTubeStrings.sectionFeed,
            FeedSettings.hideAlbumCards,
            FeedSettings.hideArtistCards,
            FeedSettings.hideChipsShelf,
            FeedSettings.hideCompactBanner,
            FeedSettings.hideCommunityPosts,
            FeedSettings.hideCreatorStoreShelf,
            FeedSettings.hideCrowdfundingBox,
            FeedSettings.hideDoodles,
            FeedSettings.hideEmergencyBox,
            FeedSettings.hideExpandableCard,
            FeedSettings.hideFeedFlyoutMenu,
            FeedSettings.hideFeedFlyoutMenuFilterStrings,
            FeedSettings.hideFilterBarFeedInFeed,
            FeedSettings.hideFilterBarFeedInHistory,
            FeedSettings.hideFilterBarFeedInRelatedVideos,
            FeedSettings.hideFilterBarFeedInSearch,
            FeedSettings.hideFloatingMicrophoneButton,
            FeedSettings.hideForYouShelf,
            FeedSettings.hideHorizontalShelves,
            FeedSettings.hideImageShelf,
            FeedSettings.hideLatestPosts,
            FeedSettings.hideLatestVideosButton,
            FeedSettings.hideLiveChatReplayButton,
            FeedSettings.hideMedicalPanels,
            FeedSettings.hideMixPlaylists,
            FeedSettings.hideMoviesSection,
            FeedSettings.hidePlayables,
            FeedSettings.hideShowMoreButton,
            FeedSettings.hideSurveys,
            FeedSettings.hideSubscribedChannelsBar,
            FeedSettings.hideTicketShelf,
            FeedSettings.hideTimedReactions,
            FeedSettings.hideVideoRecommendationLabels,
            FeedSettings.hideViewCount,
            FeedSettings.hideVisualSpacer,
            FeedSettings.hideWebSearchResults,
            FeedSettings.hideYouMayLikeSection,
        ),
        section(
            YouTubeSettingsPages.Player,
            YouTubeStrings.sectionPlayer,
            PlayerSettings.hideChannelBar,
            PlayerSettings.hideChannelWatermark,
            PlayerSettings.hideFullscreenQuickActions,
            PlayerSettings.hideInfoPanels,
            PlayerSettings.hideJoinMembershipButton,
            PlayerSettings.hideMoreVideosButton,
            PlayerSettings.hideNotifyMeButton,
            PlayerSettings.hideSubscribersCommunityGuidelines,
            PlayerSettings.hideUploadTime,
            PlayerSettings.hideVideoTitle,
        ),
        section(
            YouTubeSettingsPages.Comments,
            YouTubeStrings.sectionComments,
            CommentsSettings.hideCommentsAiChatSummary,
            CommentsSettings.hideCommentsAiSummary,
            CommentsSettings.hideCommentsByMembersHeader,
            CommentsSettings.hideCommentsChannelGuidelines,
            CommentsSettings.hideCommentsCommunityGuidelines,
            CommentsSettings.hideCommentsCreateAShortButton,
            CommentsSettings.hideCommentsEmojiAndTimestampButtons,
            CommentsSettings.hideCommentsPreviewComment,
            CommentsSettings.hideCommentsSection,
            CommentsSettings.hideCommentsSectionInHomeFeed,
            CommentsSettings.hideCommentsThanksButton,
        ),
        section(
            YouTubeSettingsPages.Description,
            YouTubeStrings.sectionDescription,
            DescriptionSettings.hideAiGeneratedVideoSummarySection,
            DescriptionSettings.hideAskSection,
            DescriptionSettings.hideAttributesSection,
            DescriptionSettings.hideChaptersSection,
            DescriptionSettings.hideCourseProgressSection,
            DescriptionSettings.hideExploreCourseSection,
            DescriptionSettings.hideExplorePodcastSection,
            DescriptionSettings.hideExploreSection,
            DescriptionSettings.hideFeaturedLinksSection,
            DescriptionSettings.hideFeaturedPlacesSection,
            DescriptionSettings.hideFeaturedVideosSection,
            DescriptionSettings.hideGamingSection,
            DescriptionSettings.hideHowThisWasMadeSection,
            DescriptionSettings.hideHypePoints,
            DescriptionSettings.hideInfoCardsSection,
            DescriptionSettings.hideKeyConceptsSection,
            DescriptionSettings.hideMusicSection,
            DescriptionSettings.hideSubscribeButton,
            DescriptionSettings.hideTranscriptSection,
            DescriptionSettings.hideQuizzesSection,
        ),
        section(
            YouTubeSettingsPages.Channel,
            YouTubeStrings.sectionChannel,
            ChannelSettings.hideChannelTab,
            ChannelSettings.hideChannelTabFilterStrings,
            ChannelSettings.hideCommunityButton,
            ChannelSettings.hideJoinButton,
            ChannelSettings.hideLinksPreview,
            ChannelSettings.hideMembersShelf,
            ChannelSettings.hideStoreButton,
            ChannelSettings.hideSubscribeButtonInChannelPage,
        ),
        section(
            YouTubeSettingsPages.Filters,
            YouTubeStrings.sectionKeywordFilter,
            FilterSettings.hideKeywordContentHome,
            FilterSettings.hideKeywordContentSubscriptions,
            FilterSettings.hideKeywordContentSearch,
            FilterSettings.hideKeywordContentPhrases,
        ),
        section(YouTubeSettingsPages.Filters, YouTubeStrings.sectionCustomFilter, YouTubeCommonSettings.customFilter, YouTubeCommonSettings.customFilterStrings),
    )

    execute {
        registerLithoFilter(LayoutComponentsFilter)
        registerLithoFilter(DescriptionComponentsFilter)
        registerLithoFilter(CommentsFilter)
        registerLithoFilter(KeywordContentFilter)

        hideShowMoreButton()
        hideSubscribedChannelsBar()
        hideMixPlaylists()
        hideChannelWatermark()
        hideFeedCards()
        hideFeedButtons()
        hideFeedSubtitle()
        hideFeedFilterBars()
        hideSearchSuggestions()
        hideFeedFlyoutMenu()
        hideChannelTabs()
    }
}
