// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.core

import app.reseam.patch.settings.Choice
import app.reseam.patch.settings.choice
import app.reseam.patch.settings.text
import app.reseam.patch.settings.toggle

// Keys derive from this object's name: `you_tube_settings.<property>`. The patched app stores
// values under them, so renaming a property resets it for everyone.
object YouTubeSettings {
    val debugLogging by toggle(
        "Debug logging",
        summary = "Writes what the Reseam patches are doing to the Android log.",
        default = false,
    )

    val featureFlagOverrides by text(
        "Feature flag overrides",
        summary = "Comma-separated `flag=value` pairs applied to YouTube's experiment flags, " +
            "for example `45419603=false`. Values are true, false, or a string.",
        default = "",
    )

    val spoofDeviceDimensions by toggle(
        "Spoof device dimensions",
        summary = "Reports a very small and a very large screen so the server offers every video quality.",
        default = false,
    )

    val pauseOnAudioInterrupt by toggle(
        "Pause on audio interrupt",
        summary = "Pauses playback instead of lowering the volume when another app plays audio.",
        default = false,
    )

    val openLinksExternally by toggle(
        "Open links externally",
        summary = "Opens links in your browser instead of the in-app browser.",
        default = true,
    )

    val sanitizeSharingLinks by toggle(
        "Sanitize sharing links",
        summary = "Strips the tracking parameters YouTube adds to a shared link.",
        default = true,
    )

    val bypassUrlRedirects by toggle(
        "Bypass URL redirects",
        summary = "Opens a link directly instead of through the youtube.com/redirect tracker.",
        default = true,
    )

    val removeViewerDiscretionDialog by toggle(
        "Remove viewer discretion dialog",
        summary = "Accepts the age-restriction warning for you. It does not bypass the restriction.",
        default = false,
    )

    val checkWatchHistoryDns by toggle(
        "Check watch history DNS",
        summary = "Warns at launch when the device DNS server blocks the domain that records watch history.",
        default = true,
    )

    val disableHdrVideo by toggle(
        "Disable HDR video",
        summary = "Tells YouTube the display has no HDR support.",
        default = false,
    )

    val forceAvcCodec by toggle(
        "Force AVC codec",
        summary = "Disables VP9, which lowers battery use and caps playback at 1080p.",
        default = false,
    )

    val changeStartPage by choice(
        "Start page",
        summary = "The page YouTube opens instead of the home feed.",
        default = "",
        choices = listOf(
            Choice("", "Home"),
            Choice("FEsubscriptions", "Subscriptions"),
            Choice("FEchannels", "All subscriptions"),
            Choice("FEexplore", "Explore"),
            Choice("FEhistory", "History"),
            Choice("FElibrary", "Library"),
            Choice("FEplaylist_aggregation", "Playlists"),
            Choice("FEactivity", "Notifications"),
            Choice("FEtrending", "Trending"),
            Choice("VLWL", "Watch later"),
            Choice("VLLL", "Liked videos"),
            Choice("com.google.android.youtube.action.open.search", "Search"),
            Choice("com.google.android.youtube.action.open.shorts", "Shorts"),
        ),
    )

    val changeStartPageAlways by toggle(
        "Always use the start page",
        summary = "Applies the start page every time the feed loads, not only on a cold start.",
        default = false,
    )

    val changeFormFactor by choice(
        "Form factor",
        summary = "The device shape YouTube reports, which decides which layout it serves.",
        default = "",
        choices = listOf(
            Choice("", "Default"),
            Choice("0", "Unknown"),
            Choice("1", "Phone"),
            Choice("2", "Tablet"),
            Choice("4", "Wearable"),
        ),
    )

    val spoofAppVersion by toggle(
        "Spoof app version",
        summary = "Tells YouTube it is an older release, which restores some old layouts.",
        default = false,
    )

    val spoofAppVersionTarget by choice(
        "Spoofed version",
        summary = "The version reported when app version spoofing is on.",
        default = "20.05.46",
        choices = listOf(
            Choice("20.13.41", "20.13.41 - restores the non-collapsed video action bar"),
            Choice("20.05.46", "20.05.46 - restores transcript functionality"),
        ),
    )

    val disableSignInToTvPopup by toggle(
        "Disable sign in to TV popup",
        summary = "Hides the prompt to sign in to a TV on the same network.",
        default = false,
    )

    val disableFullscreenAmbientMode by toggle(
        "Disable fullscreen ambient mode",
        summary = "Stops the fullscreen background from glowing with the video's colours.",
        default = true,
    )

    val bypassImageRegionRestrictions by toggle(
        "Bypass image region restrictions",
        summary = "Loads avatars and channel images from a host that is not blocked in some countries.",
        default = false,
    )

    val disableChapterSkipDoubleTap by toggle(
        "Disable chapter skip on double tap",
        summary = "Double tapping seeks by the usual amount instead of jumping to the next chapter.",
        default = false,
    )

    val hapticsChapters by toggle(
        "No haptics for chapters",
        default = false,
    )

    val hapticsPreciseSeeking by toggle(
        "No haptics for precise seeking",
        default = false,
    )

    val hapticsSeekUndo by toggle(
        "No haptics for seek undo",
        default = false,
    )

    val hapticsZoom by toggle(
        "No haptics for zoom",
        default = false,
    )

    val hideEndScreenStoreBanner by toggle(
        "Hide end-screen store banners",
        summary = "Removes shopping banners shown at the end of a video.",
        default = true,
    )

    val hideFullscreenAds by toggle(
        "Hide fullscreen ads",
        summary = "Closes fullscreen advertisements as soon as they appear.",
        default = true,
    )

    val hideGeneralAds by toggle(
        "Hide general ads",
        summary = "Removes advertising components from the home feed and search results.",
        default = true,
    )

    val hideMerchandiseBanners by toggle(
        "Hide merchandise banners",
        summary = "Removes merchandise shelves and banners from videos and channel pages.",
        default = true,
    )

    val hidePaidPromotionLabel by toggle(
        "Hide paid-promotion labels",
        summary = "Removes paid-promotion labels from video metadata.",
        default = true,
    )

    val hidePlayerPopupAds by toggle(
        "Hide player popup ads",
        summary = "Stops shopping and Premium promotion panels opening below the player.",
        default = true,
    )

    val hideSelfSponsorAds by toggle(
        "Hide self-sponsor cards",
        summary = "Removes sponsor cards that creators place in their videos.",
        default = true,
    )

    val hideShoppingLinks by toggle(
        "Hide shopping links",
        summary = "Removes shopping links shown in video descriptions.",
        default = true,
    )

    val hideViewProductsBanner by toggle(
        "Hide view-products banners",
        summary = "Removes product links and view-products banners from the player.",
        default = true,
    )

    val hideYouTubePremiumPromotions by toggle(
        "Hide YouTube Premium promotions",
        summary = "Removes YouTube Premium promotional banners and offers.",
        default = true,
    )

    val hideVideoAds by toggle(
        "Hide video ads",
        summary = "Skips advertisements served before or during video playback.",
        default = true,
    )

    // Hide layout components: feed and channel surfaces.
    val hideAlbumCards by toggle("Hide album cards", summary = "Removes album cards from feeds.", default = false)
    val hideArtistCards by toggle("Hide artist cards", summary = "Removes artist cards from feeds.", default = false)
    val hideAttributesSection by toggle("Hide video attributes", summary = "Removes attribute rows from descriptions and shelves.", default = false)
    val hideChannelBar by toggle("Hide channel bar", summary = "Removes the compact channel bar below videos.", default = false)
    val hideChannelTab by toggle("Hide channel tabs", summary = "Removes channel tabs whose names match your filter list.", default = false)
    val hideChannelTabFilterStrings by text("Channel tab names", summary = "One channel-tab name per line; matching tabs are removed.", default = "")
    val hideChannelWatermark by toggle("Hide channel watermarks", summary = "Removes channel watermarks from the player.", default = true)
    val hideChipsShelf by toggle("Hide chips shelves", summary = "Removes chips shelves from feeds.", default = true)
    val hideCommunityButton by toggle("Hide community button", summary = "Removes the Community tab button from channel pages.", default = true)
    val hideCommunityPosts by toggle("Hide community posts", summary = "Removes community-post components from feeds and channels.", default = false)
    val hideCompactBanner by toggle("Hide compact banners", summary = "Removes compact promotional banners.", default = true)
    val hideCreatorStoreShelf by toggle("Hide creator-store shelves", summary = "Removes creator merchandise shelves.", default = true)
    val hideCrowdfundingBox by toggle("Hide crowdfunding boxes", summary = "Removes crowdfunding boxes from videos.", default = false)
    val hideDoodles by toggle("Hide YouTube doodles", summary = "Removes doodles from the home feed header.", default = false)
    val hideEmergencyBox by toggle("Hide emergency boxes", summary = "Removes emergency information boxes.", default = true)
    val hideExpandableCard by toggle("Hide expandable cards", summary = "Removes expandable metadata cards.", default = true)
    val hideFeaturedPlacesSection by toggle("Hide featured places", summary = "Removes featured-place sections from descriptions.", default = false)
    val hideFeedFlyoutMenu by toggle("Hide feed flyout items", summary = "Hides feed menu items whose titles match your filter list.", default = false)
    val hideFeedFlyoutMenuFilterStrings by text("Feed flyout item names", summary = "One exact feed menu title per line.", default = "")
    val hideFilterBarFeedInFeed by toggle("Hide the feed filter bar", summary = "Removes the filter bar from the home feed.", default = false)
    val hideFilterBarFeedInHistory by toggle("Hide the history filter bar", summary = "Removes the filter bar from History.", default = false)
    val hideFilterBarFeedInRelatedVideos by toggle("Hide the related-videos filter bar", summary = "Removes the filter bar below the player.", default = false)
    val hideFilterBarFeedInSearch by toggle("Hide the search filter bar", summary = "Removes the filter bar from search results.", default = false)
    val hideFloatingMicrophoneButton by toggle("Hide the floating microphone", summary = "Removes the floating voice-search button.", default = true)
    val hideForYouShelf by toggle("Hide For You shelves", summary = "Removes For You shelves from feeds.", default = false)
    val hideGamingSection by toggle("Hide gaming sections", summary = "Removes gaming sections from descriptions.", default = false)
    val hideHorizontalShelves by toggle("Hide horizontal shelves", summary = "Removes generic horizontal shelves from feeds.", default = true)
    val hideImageShelf by toggle("Hide image shelves", summary = "Removes image shelves from feeds.", default = true)
    val hideInfoPanels by toggle("Hide information panels", summary = "Removes information panels around videos.", default = true)
    val hideJoinButton by toggle("Hide Join buttons", summary = "Removes Join buttons from channel pages.", default = false)
    val hideJoinMembershipButton by toggle("Hide membership buttons", summary = "Removes membership buttons from compact channel bars.", default = true)
    val hideLatestPosts by toggle("Hide Latest posts", summary = "Removes Latest posts shelves from feeds.", default = true)
    val hideLatestVideosButton by toggle("Hide Latest videos buttons", summary = "Removes Latest videos buttons from shelves.", default = false)
    val hideLinksPreview by toggle("Hide link previews", summary = "Removes link previews from channel pages.", default = true)
    val hideLiveChatReplayButton by toggle("Hide live-chat replay buttons", summary = "Removes live-chat replay buttons.", default = false)
    val hideMedicalPanels by toggle("Hide medical panels", summary = "Removes medical information panels.", default = true)
    val hideMembersShelf by toggle("Hide members shelves", summary = "Removes channel-members shelves.", default = true)
    val hideMixPlaylists by toggle("Hide Mix playlists", summary = "Removes automatically generated Mix playlists.", default = true)
    val hideMoviesSection by toggle("Hide movies and shows", summary = "Removes movie and show shelves and purchase prompts.", default = true)
    val hideMusicSection by toggle("Hide music sections", summary = "Removes music sections from descriptions.", default = false)
    val hideNotifyMeButton by toggle("Hide Notify me buttons", summary = "Removes Notify me buttons.", default = true)
    val hidePlayables by toggle("Hide playable shelves", summary = "Removes playable game shelves.", default = true)
    val hideQuickActions by toggle("Hide quick actions", summary = "Removes quick-action rows around videos.", default = false)
    val hideQuizzesSection by toggle("Hide quiz sections", summary = "Removes quiz sections from descriptions.", default = false)
    val hideRelatedVideos by toggle("Hide related videos", summary = "Removes related-video shelves below the player.", default = false)
    val hideShowMoreButton by toggle("Hide Show more buttons", summary = "Hides Show more in search results.", default = true)
    val hideStoreButton by toggle("Hide Store buttons", summary = "Removes Store buttons from channel pages.", default = true)
    val hideSubscribedChannelsBar by toggle("Hide the subscribed-channels bar", summary = "Removes the subscribed-channels bar from the feed.", default = false)
    val hideSubscribersCommunityGuidelines by toggle("Hide subscriber guidelines", summary = "Removes subscriber community-guideline prompts.", default = true)
    val hideSubscribeButtonInChannelPage by toggle("Hide channel Subscribe buttons", summary = "Removes Subscribe buttons on channel pages.", default = false)
    val hideSurveys by toggle("Hide surveys", summary = "Removes surveys and feed nudges.", default = true)
    val hideTicketShelf by toggle("Hide ticket shelves", summary = "Removes ticket shelves from feeds.", default = false)
    val hideTimedReactions by toggle("Hide timed reactions", summary = "Removes timed reactions and emoji controls.", default = true)
    val hideUploadTime by toggle("Hide upload times", summary = "Removes upload times from feed subtitles.", default = false)
    val hideVideoRecommendationLabels by toggle("Hide recommendation labels", summary = "Removes recommendation labels from videos.", default = true)
    val hideVideoTitle by toggle("Hide the player video title", summary = "Removes the title shown over the player.", default = false)
    val hideViewCount by toggle("Hide view counts", summary = "Removes view counts from feed subtitles.", default = false)
    val hideVisualSpacer by toggle("Hide visual spacers", summary = "Removes empty visual spacer components.", default = true)
    val hideWebSearchResults by toggle("Hide web search results", summary = "Removes web-result panels from search pages.", default = true)
    val hideYouMayLikeSection by toggle("Hide You may like", summary = "Removes trending suggestions from an empty search box.", default = true)

    // Description panel.
    val hideAiGeneratedVideoSummarySection by toggle("Hide AI video summaries", summary = "Removes AI-generated video summaries.", default = false)
    val hideAskSection by toggle("Hide Ask sections", summary = "Removes Ask sections from descriptions.", default = false)
    val hideChaptersSection by toggle("Hide chapters", summary = "Removes chapter shelves from descriptions.", default = true)
    val hideCourseProgressSection by toggle("Hide course progress", summary = "Removes course-progress sections.", default = false)
    val hideExploreCourseSection by toggle("Hide course sections", summary = "Removes course sections from descriptions.", default = false)
    val hideExplorePodcastSection by toggle("Hide podcast sections", summary = "Removes podcast sections from descriptions.", default = false)
    val hideExploreSection by toggle("Hide Explore sections", summary = "Removes Explore sections from descriptions.", default = true)
    val hideFeaturedLinksSection by toggle("Hide featured links", summary = "Removes featured links from descriptions.", default = false)
    val hideFeaturedVideosSection by toggle("Hide featured videos", summary = "Removes featured videos from descriptions.", default = false)
    val hideHowThisWasMadeSection by toggle("Hide How this was made", summary = "Removes How this was made sections.", default = false)
    val hideHypePoints by toggle("Hide Hype points", summary = "Removes Hype points from descriptions.", default = false)
    val hideInfoCardsSection by toggle("Hide info cards", summary = "Removes information-card sections from descriptions.", default = true)
    val hideKeyConceptsSection by toggle("Hide key concepts", summary = "Removes key-concept shelves from descriptions.", default = false)
    val hideSubscribeButton by toggle("Hide description Subscribe buttons", summary = "Removes Subscribe buttons inside descriptions.", default = false)
    val hideTranscriptSection by toggle("Hide transcripts", summary = "Removes transcript sections from descriptions.", default = true)

    // Comments.
    val hideCommentsAiChatSummary by toggle("Hide AI chat summaries", summary = "Removes AI summaries from comments.", default = false)
    val hideCommentsAiSummary by toggle("Hide comment AI summaries", summary = "Removes AI summary chips from comments.", default = false)
    val hideCommentsByMembersHeader by toggle("Hide members headers in comments", summary = "Removes members headers from comments.", default = false)
    val hideCommentsChannelGuidelines by toggle("Hide comment channel guidelines", summary = "Removes channel guidelines from comments.", default = true)
    val hideCommentsCommunityGuidelines by toggle("Hide comment community guidelines", summary = "Removes community guidelines from comments.", default = true)
    val hideCommentsCreateAShortButton by toggle("Hide Create a Short in comments", summary = "Removes the Create a Short button from comments.", default = true)
    val hideCommentsEmojiAndTimestampButtons by toggle("Hide comment emoji and timestamp buttons", summary = "Removes emoji and timestamp actions from the comment composer.", default = false)
    val hideCommentsPreviewComment by toggle("Hide comment previews", summary = "Removes comment previews from feeds.", default = false)
    val hideCommentsSection by toggle("Hide comments", summary = "Removes the comments section from videos.", default = false)
    val hideCommentsSectionInHomeFeed by toggle("Hide comments in the home feed", summary = "Removes comment sections attached to home-feed videos.", default = false)
    val hideCommentsThanksButton by toggle("Hide Thanks buttons in comments", summary = "Removes Thanks buttons from comments.", default = true)

    // Keyword and custom filters.
    val hideKeywordContentHome by toggle("Filter keywords on Home", summary = "Hides home-feed videos whose buffer contains a configured phrase.", default = false)
    val hideKeywordContentSubscriptions by toggle("Filter keywords in Subscriptions", summary = "Hides subscription videos whose buffer contains a configured phrase.", default = false)
    val hideKeywordContentSearch by toggle("Filter keywords in Search", summary = "Hides search results whose buffer contains a configured phrase.", default = false)
    val hideKeywordContentPhrases by text("Keyword phrases", summary = "One phrase per line. Quote a phrase to match whole words only.", default = "")
    val customFilter by toggle("Enable custom component filter", summary = "Enables expressions in the custom component-filter field.", default = false)
    val customFilterStrings by text("Custom component filters", summary = "One expression per line: ^path#accessibility\$buffer.", default = "")

    val hideShortsAiButton by toggle(
        "Hide Shorts AI button",
        summary = "Hides the AI button in the Shorts player.",
        default = false,
    )

    val hideShortsAutoDubbedLabel by toggle(
        "Hide Shorts auto-dubbed label",
        summary = "Hides the auto-dubbed label in Shorts.",
        default = false,
    )

    val hideShortsChannel by toggle(
        "Hide Shorts channel",
        summary = "Hides the channel identity in Shorts shelves and the player.",
        default = false,
    )

    val hideShortsChannelBar by toggle(
        "Hide Shorts channel bar",
        summary = "Hides the channel action bar in the Shorts player.",
        default = false,
    )

    val hideShortsCommentsButton by toggle(
        "Hide Shorts comments button",
        summary = "Hides the comments button in the Shorts player.",
        default = false,
    )

    val hideShortsDislikeButton by toggle(
        "Hide Shorts dislike button",
        summary = "Hides the dislike button in the Shorts player.",
        default = false,
    )

    val hideShortsFullVideoLinkLabel by toggle(
        "Hide Shorts full-video link",
        summary = "Hides the link to open a Short as a regular video.",
        default = false,
    )

    val hideShortsEffectButton by toggle(
        "Hide Shorts effect button",
        summary = "Hides the effect button in the Shorts player.",
        default = true,
    )

    val hideShortsGreenScreenButton by toggle(
        "Hide Shorts green-screen button",
        summary = "Hides the green-screen button in the Shorts player.",
        default = true,
    )

    val hideShortsNewPostsButton by toggle(
        "Hide Shorts new-posts button",
        summary = "Hides the new-posts button in the Shorts player.",
        default = true,
    )

    val hideShortsHashtagButton by toggle(
        "Hide Shorts hashtag button",
        summary = "Hides the hashtag button in the Shorts player.",
        default = true,
    )

    val hideShortsHome by toggle(
        "Hide Shorts shelf",
        summary = "Removes Shorts shelves from the Home feed.",
        default = false,
    )

    val hideShortsInfoPanel by toggle(
        "Hide Shorts info panel",
        summary = "Hides the information panel in the Shorts player.",
        default = true,
    )

    val hideShortsJoinButton by toggle(
        "Hide Shorts Join button",
        summary = "Hides the Join button in the Shorts player.",
        default = true,
    )

    val hideShortsLikeButton by toggle(
        "Hide Shorts like button",
        summary = "Hides the like button in the Shorts player.",
        default = false,
    )

    val hideShortsLikeFountain by toggle(
        "Hide Shorts like fountain",
        summary = "Hides the like animation in the Shorts player.",
        default = true,
    )

    val hideShortsLivePreview by toggle(
        "Hide Shorts live preview",
        summary = "Hides live previews in the Shorts player.",
        default = false,
    )

    val hideShortsLocationLabel by toggle(
        "Hide Shorts location label",
        summary = "Hides the location label in the Shorts player.",
        default = false,
    )

    val hideShortsPausedOverlayButtons by toggle(
        "Hide Shorts paused-overlay buttons",
        summary = "Hides action buttons shown when a Short is paused.",
        default = false,
    )

    val hideShortsPreviewComment by toggle(
        "Hide Shorts preview comment",
        summary = "Hides the comment preview in the Shorts player.",
        default = true,
    )

    val hideShortsRemixButton by toggle(
        "Hide Shorts remix button",
        summary = "Hides the Remix button in the Shorts player.",
        default = false,
    )

    val hideShortsSaveSoundButton by toggle(
        "Hide Shorts save-sound button",
        summary = "Hides the buttons for saving a sound from a Short.",
        default = true,
    )

    val hideShortsSearchSuggestions by toggle(
        "Hide Shorts search suggestions",
        summary = "Hides search suggestions in the Shorts player.",
        default = true,
    )

    val hideShortsShareButton by toggle(
        "Hide Shorts share button",
        summary = "Hides the share button in the Shorts player.",
        default = false,
    )

    val hideShortsShopButton by toggle(
        "Hide Shorts shop button",
        summary = "Hides shopping buttons in the Shorts player.",
        default = true,
    )

    val hideShortsSoundButton by toggle(
        "Hide Shorts sound button",
        summary = "Hides the sound button in the Shorts player.",
        default = false,
    )

    val hideShortsSoundMetadataLabel by toggle(
        "Hide Shorts sound metadata",
        summary = "Hides sound metadata in the Shorts player.",
        default = false,
    )

    val hideShortsStickers by toggle(
        "Hide Shorts stickers",
        summary = "Hides stickers in the Shorts player.",
        default = true,
    )

    val hideShortsSubscribeButton by toggle(
        "Hide Shorts Subscribe button",
        summary = "Hides the Subscribe button in the Shorts player.",
        default = true,
    )

    val hideShortsSuperThanksButton by toggle(
        "Hide Shorts Super Thanks button",
        summary = "Hides the Super Thanks button in the Shorts player.",
        default = true,
    )

    val hideShortsTaggedProducts by toggle(
        "Hide Shorts tagged products",
        summary = "Hides tagged products in the Shorts player.",
        default = true,
    )

    val hideShortsUpcomingButton by toggle(
        "Hide Shorts upcoming button",
        summary = "Hides the upcoming button in the Shorts player.",
        default = true,
    )

    val hideShortsUseSoundButton by toggle(
        "Hide Shorts Use sound button",
        summary = "Hides the Use sound button in the Shorts player.",
        default = true,
    )

    val hideShortsUseTemplateButton by toggle(
        "Hide Shorts Use template button",
        summary = "Hides the Use template button in the Shorts player.",
        default = true,
    )

    val hideShortsVideoDescription by toggle(
        "Hide Shorts video description",
        summary = "Hides Shorts shelves in the video-description panel.",
        default = false,
    )

    val hideShortsVideoTitle by toggle(
        "Hide Shorts video title",
        summary = "Hides the title in the Shorts player.",
        default = false,
    )

    val shortsAutoplay by toggle(
        "Shorts autoplay",
        summary = "Automatically plays the next Short.",
        default = false,
    )

    val shortsAutoplayBackground by toggle(
        "Shorts autoplay in background",
        summary = "Keeps Shorts autoplay enabled when YouTube is not in the foreground.",
        default = true,
    )

    val dimShortsOverlayOpacity by text(
        "Shorts overlay opacity",
        summary = "Opacity of the Shorts player overlay, from 0 to 100.",
        default = "90",
    )

    val dimShortsOverlayImmersiveMode by toggle(
        "Shorts immersive mode",
        summary = "Hides the status bar while watching Shorts.",
        default = true,
    )

    val disableResumingShortsOnStartup by toggle(
        "Disable resuming Shorts on startup",
        summary = "Stops YouTube reopening the last Short when it starts.",
        default = false,
    )

    val openVideosFullscreen by toggle(
        "Open videos fullscreen",
        summary = "Opens videos in fullscreen portrait mode when YouTube starts them.",
        default = false,
    )

    val hideAutoplayButton by toggle(
        "Hide autoplay button",
        summary = "Removes the autoplay toggle from the player controls.",
        default = true,
    )

    val hideCaptionsButton by toggle(
        "Hide captions button",
        summary = "Removes the captions button from the player controls.",
        default = false,
    )

    val hideCastButton by toggle(
        "Hide cast button",
        summary = "Removes the cast button from the player and feed.",
        default = true,
    )

    val hideCollapseButton by toggle(
        "Hide collapse button",
        summary = "Removes the button that collapses the player.",
        default = false,
    )

    val hideFullscreenButton by toggle(
        "Hide fullscreen button",
        summary = "Removes the fullscreen button from the player controls.",
        default = false,
    )

    val hidePlayerControlButtonsBackground by toggle(
        "Hide player control button background",
        summary = "Removes the background behind the player control buttons.",
        default = false,
    )

    val hidePlayerPreviousNextButtons by toggle(
        "Hide previous and next buttons",
        summary = "Removes the previous and next buttons from the player controls.",
        default = false,
    )

    val hideAutoplayPreview by toggle(
        "Hide autoplay preview",
        summary = "Stops the autoplay preview from appearing over the player.",
        default = false,
    )

    val hideEndScreenCards by toggle(
        "Hide end-screen cards",
        summary = "Removes cards shown over the video at the end.",
        default = false,
    )

    val hideEndScreenSuggestedVideo by toggle(
        "Hide end-screen suggested video",
        summary = "Removes the suggested video shown at the end of playback.",
        default = false,
    )

    val hideRelatedVideoOverlay by toggle(
        "Hide related-video overlay",
        summary = "Removes related videos shown over the player.",
        default = false,
    )

    val hideTimestamp by toggle(
        "Hide timestamp",
        summary = "Removes the elapsed and remaining time from the player controls.",
        default = false,
    )

    val disablePlayerPopupPanels by toggle(
        "Disable player popup panels",
        summary = "Stops popup panels from opening from the player.",
        default = false,
    )

    val disableRollingNumberAnimations by toggle(
        "Disable rolling number animations",
        summary = "Stops animated number transitions in the player.",
        default = false,
    )

    val playerOverlayOpacity by text(
        "Player overlay opacity",
        summary = "Sets the player overlay opacity from 0 to 100 percent.",
        default = "100",
    )

    val disableAutoCaptions by toggle(
        "Disable auto captions",
        summary = "Prevents captions from being enabled automatically.",
        default = false,
    )

    val disableLikeSubscribeGlow by toggle(
        "Disable like and subscribe glow",
        summary = "Stops the animated glow around the like and subscribe buttons.",
        default = false,
    )

    val hideLikeDislikeButton by toggle(
        "Hide like and dislike button",
        default = false,
    )

    val hideDownloadButton by toggle(
        "Hide download button",
        default = false,
    )

    val hideRemixButton by toggle(
        "Hide remix button",
        default = false,
    )

    val hideSaveButton by toggle(
        "Hide save button",
        default = false,
    )

    val hideShareButton by toggle(
        "Hide share button",
        default = false,
    )

    val hideInfoCards by toggle(
        "Hide info cards",
        summary = "Removes information cards shown over videos.",
        default = false,
    )

    val hidePlayerFlyoutCaptions by toggle(
        "Hide flyout captions",
        default = false,
    )

    val hidePlayerFlyoutListenWithYoutubeMusic by toggle(
        "Hide flyout Listen with YouTube Music",
        default = false,
    )

    val hidePlayerFlyoutHelp by toggle(
        "Hide flyout help",
        default = true,
    )

    val hidePlayerFlyoutLockScreen by toggle(
        "Hide flyout lock screen",
        default = false,
    )

    val hidePlayerFlyoutSpeed by toggle(
        "Hide flyout playback speed",
        default = false,
    )

    val hidePlayerFlyoutAudioTrack by toggle(
        "Hide flyout audio track",
        default = false,
    )

    val hidePlayerFlyoutAdditionalSettings by toggle(
        "Hide flyout additional settings",
        default = false,
    )

    val hidePlayerFlyoutAmbientMode by toggle(
        "Hide flyout ambient mode",
        default = false,
    )

    val hidePlayerFlyoutLoopVideo by toggle(
        "Hide flyout loop video",
        default = false,
    )

    val hidePlayerFlyoutStableVolume by toggle(
        "Hide flyout stable volume",
        default = false,
    )

    val hidePlayerFlyoutSleepTimer by toggle(
        "Hide flyout sleep timer",
        default = false,
    )

    val hidePlayerFlyoutWatchInVr by toggle(
        "Hide flyout watch in VR",
        default = false,
    )

    val hidePlayerFlyoutVideoQuality by toggle(
        "Hide flyout video quality",
        default = false,
    )

    val hidePlayerFlyoutVideoQualityFooter by toggle(
        "Hide flyout video quality footer",
        default = false,
    )

    val hideHomeButton by toggle(
        "Hide home button",
        summary = "Hides the Home button in the bottom navigation bar.",
        default = false,
    )

    val hideShortsButton by toggle(
        "Hide Shorts button",
        summary = "Hides the Shorts button in the bottom navigation bar.",
        default = true,
    )

    val hideCreateButton by toggle(
        "Hide create button",
        summary = "Hides the Create button in the bottom navigation bar.",
        default = true,
    )

    val hideSubscriptionsButton by toggle(
        "Hide subscriptions button",
        summary = "Hides the Subscriptions button in the bottom navigation bar.",
        default = false,
    )

    val hideNotificationsButton by toggle(
        "Hide notifications button",
        summary = "Hides the Notifications button in the bottom navigation bar.",
        default = false,
    )

    val hideLibraryButton by toggle(
        "Hide You button",
        summary = "Hides the You button in the bottom navigation bar.",
        default = false,
    )

    val switchCreateWithNotificationsButton by toggle(
        "Switch create with notifications",
        summary = "Uses the Notifications button in place of Create in the bottom navigation bar.",
        default = true,
    )

    val hideNavigationButtonLabels by toggle(
        "Hide navigation button labels",
        summary = "Hides the text labels below the bottom navigation buttons.",
        default = false,
    )

    val narrowNavigationButtons by toggle(
        "Narrow navigation buttons",
        summary = "Uses narrower spacing for the bottom navigation buttons.",
        default = false,
    )

    val navigationBarAnimations by toggle(
        "Navigation bar animations",
        summary = "Enables animations when switching bottom navigation buttons.",
        default = false,
    )

    val disableTranslucentStatusBar by toggle(
        "Disable translucent status bar",
        summary = "Disables the translucent status bar effect used by the navigation bar.",
        default = false,
    )

    val disableTranslucentNavigationBarLight by toggle(
        "Disable light navigation bar translucency",
        summary = "Disables translucent navigation buttons in light mode.",
        default = false,
    )

    val disableTranslucentNavigationBarDark by toggle(
        "Disable dark navigation bar translucency",
        summary = "Disables translucent navigation buttons in dark mode.",
        default = false,
    )

    val hideToolbarCreateButton by toggle(
        "Hide toolbar create button",
        summary = "Hides the Create button in the top toolbar.",
        default = true,
    )

    val hideToolbarNotificationButton by toggle(
        "Hide toolbar notification button",
        summary = "Hides the Notifications button in the top toolbar.",
        default = false,
    )

    val hideToolbarSearchButton by toggle(
        "Hide toolbar search button",
        summary = "Hides the Search button in the top toolbar.",
        default = false,
    )

    val hideMiniplayerOverlayButtons by toggle(
        "Hide miniplayer overlay buttons",
        summary = "Hides the miniplayer expand, close and action buttons.",
        default = false,
    )

    val hideMiniplayerSubtext by toggle(
        "Hide miniplayer subtext",
        summary = "Hides the miniplayer subtitle.",
        default = false,
    )

    val miniplayerWidthDip by text(
        "Miniplayer width",
        summary = "The miniplayer width in dp; values are clamped to the phone's usable range.",
        default = "192",
    )

    val gradientLoadingScreen by toggle(
        "Gradient loading screen",
        summary = "Uses YouTube's gradient while the app is loading.",
        default = false,
    )

    val splashScreenAnimationStyle by choice(
        "Splash screen animation",
        summary = "Chooses the animation YouTube shows while starting.",
        default = "1",
        choices = listOf(
            Choice("0", "Disabled"),
            Choice("1", "60 FPS, 1 second"),
            Choice("2", "60 FPS, 2 seconds"),
            Choice("3", "60 FPS, 5 seconds"),
            Choice("4", "60 FPS, black and white"),
            Choice("5", "30 FPS, 1 second"),
            Choice("6", "30 FPS, 2 seconds"),
            Choice("7", "30 FPS, 5 seconds"),
            Choice("8", "30 FPS, black and white"),
        ),
    )

    val seekbarCustomColor by toggle(
        "Custom seekbar color",
        summary = "Replaces the red video seekbar with a color of your choice.",
        default = false,
    )

    val seekbarCustomColorPrimary by text(
        "Primary seekbar color",
        summary = "A color such as #FF0000 used for the played portion of the seekbar.",
        default = "#FFFF0033",
    )

    val seekbarCustomColorAccent by text(
        "Seekbar accent color",
        summary = "A color such as #FF2791 used for the seekbar gradient.",
        default = "#FFFF2791",
    )

    val hideSeekbar by toggle(
        "Hide seekbar",
        summary = "Hides the video player seekbar and its controls.",
        default = false,
    )

    val hideSeekbarThumbnail by toggle(
        "Hide thumbnail progress bars",
        summary = "Hides playback progress bars on video thumbnails.",
        default = false,
    )


    val slideToSeek by toggle(
        "Slide to seek",
        summary = "Slides to seek instead of playing at 2x speed when holding the player.",
        default = false,
    )

    val tapToSeek by toggle(
        "Tap to seek",
        summary = "Seeks to the tapped position on the video player seekbar.",
        default = false,
    )

    val disablePreciseSeekingGesture by toggle(
        "Disable precise seeking gesture",
        summary = "Stops swiping up on the seekbar from entering precise seeking mode.",
        default = false,
    )

    val customBrandingName by choice(
        "App name",
        summary = "Chooses the name shown by the selected launcher icon.",
        default = "2",
        choices = listOf(
            Choice("1", "YouTube"),
            Choice("2", "YouTube Reseam"),
            Choice("3", "YT Reseam"),
            Choice("4", "YT"),
        ),
    )

    val customBrandingIcon by choice(
        "App icon",
        summary = "Chooses the launcher icon used by the patched app.",
        default = "original",
        choices = listOf(
            Choice("original", "Original"),
            Choice("reseam", "Reseam"),
        ),
    )

    val changeHeaderLogo by choice(
        "Header logo",
        summary = "Chooses the logo shown in YouTube's top header.",
        default = "default",
        choices = listOf(
            Choice("default", "Default"),
            Choice("premium", "Premium"),
            Choice("reseam", "Reseam"),
        ),
    )

    val copyVideoUrl by toggle("Copy video URL button", summary = "Adds a player button that copies the current video URL.", default = false)
    val copyVideoUrlTimestamp by toggle("Copy video URL with timestamp", summary = "Adds a player button that copies the current URL with its playback timestamp.", default = true)
    val externalDownloader by toggle("External downloader button", summary = "Adds a player button that sends the video URL to an external downloader.", default = false)
    val externalDownloaderActionButton by toggle("External downloader action", summary = "Uses the external downloader for YouTube's in-app Download action.", default = false)
    val externalDownloaderPackageName by text("External downloader package", summary = "Android package name of the external downloader app.", default = "com.deniscerri.ytdl")
    val loopVideo by toggle("Loop video", summary = "Starts the video again when it reaches the end.", default = false)
    val loopVideoButton by toggle("Loop video button", summary = "Adds a player button for toggling video looping.", default = false)

    // Video quality, playback speed, audio and background playback.
    val videoQualityDefault by choice(
        "Default video quality",
        summary = "The highest resolution YouTube should select automatically for new videos.",
        default = "-2",
        choices = listOf(
            Choice("-2", "Auto"),
            Choice("360", "360p"),
            Choice("480", "480p"),
            Choice("720", "720p"),
            Choice("1080", "1080p"),
            Choice("1440", "1440p"),
            Choice("2160", "2160p"),
        ),
    )
    val rememberVideoQuality by toggle(
        "Remember video quality",
        summary = "Remembers the last quality selected for later videos.",
        default = false,
    )
    val advancedVideoQualityMenu by toggle(
        "Advanced video quality menu",
        summary = "Opens YouTube's detailed quality menu instead of the quick menu.",
        default = true,
    )
    val hidePremiumVideoQuality by toggle(
        "Hide Premium video quality",
        summary = "Removes Premium-only quality choices from the quality menu.",
        default = true,
    )
    val videoQualityDialogButton by toggle(
        "Video quality player button",
        summary = "Adds a quality button to the bottom player controls.",
        default = false,
    )
    val customPlaybackSpeedMenu by toggle(
        "Custom playback speed menu",
        summary = "Shows the custom speed choices when YouTube opens its speed menu.",
        default = true,
    )
    val restoreOldPlaybackSpeedMenu by toggle(
        "Restore old playback speed menu",
        summary = "Uses a compact list of custom speeds instead of the modern speed dialog.",
        default = false,
    )
    val customPlaybackSpeeds by text(
        "Custom playback speeds",
        summary = "Whitespace-separated playback speeds from 0.05x through 8x.",
        default = "0.25 0.5 0.75 1.0 1.25 1.5 1.75 2.0 2.5 3.0 4.0 5.0 6.0 7.0 8.0",
    )
    val playbackSpeedTapAndHold by text(
        "Tap-and-hold playback speed",
        summary = "The speed used while holding the seek gesture.",
        default = "2.0",
    )
    val playbackSpeedDefault by text(
        "Default playback speed",
        summary = "The speed applied when a new video starts, or 0 to leave it unchanged.",
        default = "0",
    )
    val rememberPlaybackSpeed by toggle(
        "Remember playback speed",
        summary = "Remembers the last playback speed selected for later videos.",
        default = false,
    )
    val playbackSpeedDialogButton by toggle(
        "Playback speed player button",
        summary = "Adds a speed button to the bottom player controls.",
        default = false,
    )
    val forceOriginalAudio by toggle(
        "Force original audio",
        summary = "Prefers the original audio track over dubbed tracks.",
        default = true,
    )
    val removeBackgroundPlaybackRestrictions by toggle(
        "Remove background playback restrictions",
        summary = "Allows eligible videos to continue playing when the app is backgrounded.",
        default = true,
    )
    val allowShortsBackgroundPlayback by toggle(
        "Allow Shorts background playback",
        summary = "Allows Shorts to continue playing when the app is backgrounded.",
        default = true,
    )

    // Stream, request and account compatibility.
    val spoofVideoStreams by toggle(
        "Spoof video streams",
        summary = "Requests playback streams using a compatible alternate YouTube client.",
        default = true,
    )
    val spoofVideoStreamsClient by choice(
        "Default client",
        summary = "The client asked first for playback streams; the others are tried when it returns none.",
        default = "android_reel_no_auth",
        choices = listOf(
            Choice("android_reel", "Android Reel"),
            Choice("android_reel_no_auth", "Android Reel (no auth)"),
            Choice("android_creator", "Android Studio"),
            Choice("android_vr_1_43", "Android VR"),
            Choice("visionos", "visionOS"),
        ),
    )
    val spoofVideoStreamsAv1 by toggle(
        "Allow Android VR AV1",
        summary = "With the Android VR client, also allows the AV1 codec, which may use software decoding.",
        default = false,
    )
    val spoofVideoStreamsStatsForNerds by toggle(
        "Show spoofed client in stats",
        summary = "Adds the active stream client to the player statistics format line.",
        default = true,
    )
    val userAgentClientSpoof by toggle(
        "Spoof user-agent client",
        summary = "Reports YouTube's original package name in generated client user agents.",
        default = true,
    )
    val accountCredentialsInvalidText by toggle(
        "Explain invalid account credentials",
        summary = "Replaces a misleading offline error when the network is actually available.",
        default = true,
    )
}
