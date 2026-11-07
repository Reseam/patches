// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.hidelayout

import app.reseam.runtime.ToggleValue
import app.reseam.youtube.litho.FilterGroup.ByteArrayFilterGroup
import app.reseam.youtube.litho.FilterGroup.StringFilterGroup
import app.reseam.youtube.litho.FilterGroupList.StringFilterGroupList
import app.reseam.youtube.litho.FilterRule
import app.reseam.youtube.litho.LithoComponent
import app.reseam.youtube.navigation.NavigationBar
import app.reseam.youtube.player.PlayerType
import app.reseam.youtube.settings.AdsSettings
import app.reseam.youtube.settings.ChannelSettings
import app.reseam.youtube.settings.FeedSettings
import app.reseam.youtube.settings.PlayerSettings

internal class LayoutFilterGroups(private val filtering: LayoutFiltering) {
    private val innerButton = StringFilterGroup(null, "|button.e")
    private val joinMembershipButton = ByteArrayFilterGroup(null, "sponsorships")
    private val buyMovieBuffer = ByteArrayFilterGroup(null, "FEstorefront")
    private val premiumPageCommand = ByteArrayFilterGroup(null, "SPunlimited")
    private val chips = feedRule(FeedSettings.hideChipsShelf, "chips_shelf")
    private val channelProfileGroups =
        StringFilterGroupList().apply {
            addAll(
                StringFilterGroup(ChannelSettings.hideCommunityButton, "community_button"),
                StringFilterGroup(ChannelSettings.hideJoinButton, "sponsor_button"),
                StringFilterGroup(ChannelSettings.hideStoreButton, "header_store_button"),
                StringFilterGroup(
                    ChannelSettings.hideSubscribeButtonInChannelPage,
                    "subscribe_button",
                ),
            )
        }

    val identifierGroups =
        arrayOf(
            chips,
            feedRule(FeedSettings.hideLiveChatReplayButton, "live_chat_ep_entrypoint.e"),
            feedRule(FeedSettings.hideVisualSpacer, "cell_divider"),
        )

    val pathGroups =
        arrayOf(
            feedRule(FeedSettings.hideArtistCards, "official_card"),
            feedRule(null, "multi_feed_icon_button"),
            feedRule(ChannelSettings.hideLinksPreview, "attribution.e"),
            feedRule(ChannelSettings.hideMembersShelf, "member_recognition_shelf"),
            FilterRule(null, "channel_profile.e", "page_header.e") {
                channelProfileGroups.check(it.accessibility).isFiltered ||
                    hideHeaderActionButton(it)
            },
            FilterRule(FeedSettings.hideGetPremiumButton, "flexible_actions.e") {
                it.endsAt("button.e") && premiumPageCommand.check(it.elementBuffer).isFiltered
            },
            feedRule(PlayerSettings.hideChannelWatermark, "featured_channel_watermark_overlay"),
            feedRule(FeedSettings.hideFilterBarFeedInHistory, "chip_bar") {
                it.index == 0 &&
                    NavigationBar.NavigationButton.selectedNavigationButton ==
                        NavigationBar.NavigationButton.LIBRARY
            },
            feedRule(FeedSettings.hideCompactBanner, "compact_banner"),
            feedRule(PlayerSettings.hideChannelBar, "compact_channel_bar"),
            feedRule(PlayerSettings.hideJoinMembershipButton, "compact_channel_bar_inner") {
                innerButton.check(it.path).isFiltered &&
                    joinMembershipButton.check(it.buffer).isFiltered
            },
            FilterRule(
                FeedSettings.hideCommunityPosts,
                "post_base_wrapper",
                "text_post_root.e",
                "images_post_root.e",
                "images_post_slim.e",
                "images_post_root_slim.e",
                "text_post_root_slim.e",
                "post_base_wrapper_slim.e",
                "poll_post_root.e",
                "videos_post_root.e",
                "post_shelf_slim.e",
                "videos_post_responsive_root.e",
                "text_post_responsive_root.e",
                "poll_post_responsive_root.e",
                "shared_post_root.e",
            ) {
                !isChannelBrowsePage() && filtering.allowsFeedPath(it.path)
            },
            feedRule(FeedSettings.hideCrowdfundingBox, "donation_shelf"),
            feedRule(FeedSettings.hideEmergencyBox, "emergency_onebox"),
            FilterRule(FeedSettings.hideExpandableCard, "inline_expander"),
            feedRule(FeedSettings.hideForYouShelf, "mixed_content_shelf"),
            feedRule(
                null,
                "horizontal_video_shelf.e",
                "horizontal_shelf.e",
                "horizontal_shelf_inline.e",
                "horizontal_tile_shelf.e",
                accepts = filtering::hideHorizontalShelf,
            ),
            chips,
            feedRule(FeedSettings.hideImageShelf, "image_shelf"),
            feedRule(PlayerSettings.hideInfoPanels, "publisher_transparency_panel"),
            feedRule(FeedSettings.hideLatestPosts, "post_shelf"),
            feedRule(FeedSettings.hideMedicalPanels, "medical_panel"),
            FilterRule(
                FeedSettings.hideMoviesSection,
                "browsy_bar",
                "compact_movie",
                "compact_tvfilm_item",
                "horizontal_movie_shelf",
                "movie_and_show_upsell_card",
                "offer_module_root",
                "video_lockup_with_attachment.e",
            ) {
                // Attachments share the lockup's identifier and buffer; only the lockup is an
                // offer.
                when {
                    "video_lockup_with_attachment.e" !in it.path -> true
                    it.path != "${it.identifier}|CellType|" -> false
                    else -> buyMovieBuffer.check(it.buffer).isFiltered
                }
            },
            FilterRule(PlayerSettings.hideNotifyMeButton, "set_reminder_button"),
            feedRule(AdsSettings.hidePaidPromotionLabel, "paid_content_overlay"),
            feedRule(FeedSettings.hidePlayables, "horizontal_gaming_shelf.e", "mini_game_card.e"),
            feedRule(PlayerSettings.hideFullscreenQuickActions, "quick_actions"),
            feedRule(PlayerSettings.hideMoreVideosButton, "fullscreen_related_videos"),
            FilterRule(PlayerSettings.hideInfoPanels, "single_item_information_panel") {
                PlayerType.current.isMaximizedOrFullscreen || !NavigationBar.isSearchBarActive
            },
            feedRule(FeedSettings.hideSubscribedChannelsBar, "subscriptions_channel_bar"),
            feedRule(
                PlayerSettings.hideSubscribersCommunityGuidelines,
                "sponsorships_comments_upsell",
            ),
            feedRule(FeedSettings.hideFilterBarFeedInFeed, "subscriptions_chip_bar"),
            FilterRule(FeedSettings.hideSurveys, "in_feed_survey", "slimline_survey", "feed_nudge"),
            feedRule(FeedSettings.hideTimedReactions, "emoji_control_panel", "timed_reaction"),
            feedRule(PlayerSettings.hideVideoTitle, "player_overlay_video_heading.e"),
            feedRule(FeedSettings.hideVideoRecommendationLabels, "endorsement_header_footer.e"),
            feedRule(
                FeedSettings.hideWebSearchResults,
                "web_link_panel",
                "web_result_panel",
            ),
        )

    private fun feedRule(
        setting: ToggleValue?,
        vararg patterns: String,
        accepts: (LithoComponent) -> Boolean = { true },
    ): FilterRule =
        FilterRule(setting, *patterns) { filtering.allowsFeedPath(it.path) && accepts(it) }
}

// A plain button can represent several header actions. Every kind present must be hidden.
private fun hideHeaderActionButton(content: LithoComponent): Boolean {
    val row = content.path.indexOf("channel_action_buttons_phone.e")
    if (row < 0 || content.path.indexOf("|button.e", row) < 0) return false
    val present =
        headerActions.asSequence().filter { it.marker.check(content.buffer).isFiltered }.iterator()
    return present.hasNext() && present.asSequence().all { it.setting.value }
}

private fun isChannelBrowsePage(): Boolean =
    NavigationBar.isBackButtonVisible &&
        !NavigationBar.isSearchBarActive &&
        PlayerType.current != PlayerType.WATCH_WHILE_MAXIMIZED

private data class HeaderAction(val marker: ByteArrayFilterGroup, val setting: ToggleValue)

private val headerActions =
    arrayOf(
        HeaderAction(
            ByteArrayFilterGroup(null, "eml.header_community_button"),
            ChannelSettings.hideCommunityButton,
        ),
        HeaderAction(
            ByteArrayFilterGroup(null, "id.sponsor_button"),
            ChannelSettings.hideJoinButton,
        ),
    )
