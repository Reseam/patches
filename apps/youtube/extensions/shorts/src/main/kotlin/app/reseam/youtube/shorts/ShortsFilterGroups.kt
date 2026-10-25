// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.shorts

import app.reseam.youtube.litho.FilterGroup.ByteArrayFilterGroup
import app.reseam.youtube.litho.FilterGroupList.ByteArrayFilterGroupList
import app.reseam.youtube.litho.FilterRule
import app.reseam.youtube.litho.LithoComponent
import app.reseam.youtube.player.EngagementPanel
import app.reseam.youtube.player.PlayerType
import app.reseam.youtube.settings.AdsSettings
import app.reseam.youtube.settings.ShortsSettings

private val LithoComponent.isPlayerOverlay: Boolean
    get() = playerOverlayPrefixes.any(path::startsWith)

private val playerOverlayPrefixes =
    listOf("reel_channel_bar.e", "reel_metapanel.e", "reel_player_overlay.e")

internal class ShortsFilterGroups {
    private val channelHeaderBuffer =
        ByteArrayFilterGroup(ShortsSettings.hideShortsChannel, "Shorts")
    private val compactVideoBuffer = ByteArrayFilterGroup(null, "/frame0.jpg")
    private val useSoundBuffer = ByteArrayFilterGroup(null, "yt_outline_camera_")
    private val useTemplateBuffer = ByteArrayFilterGroup(null, "yt_outline_template_add_")
    private val reelCarouselBuffer =
        ByteArrayFilterGroupList().apply {
            addAll(
                ByteArrayFilterGroup(
                    ShortsSettings.hideShortsAiButton,
                    "yt_outline_info_circle",
                    "yt_outline_experimental_info_circle",
                ),
                ByteArrayFilterGroup(
                    ShortsSettings.hideShortsSoundMetadataLabel,
                    "yt_outline_audio",
                    "yt_outline_experimental_audio",
                ),
            )
        }
    private val suggestedActionsBuffer =
        ByteArrayFilterGroupList().apply {
            addAll(
                ByteArrayFilterGroup(
                    ShortsSettings.hideShortsPreviewComment,
                    "shorts-comments-panel",
                ),
                ByteArrayFilterGroup(
                    ShortsSettings.hideShortsShopButton,
                    "yt_outline_bag_",
                    "yt_outline_experimental_bag_",
                ),
                ByteArrayFilterGroup(ShortsSettings.hideShortsTaggedProducts, "PAproduct_listZ"),
                ByteArrayFilterGroup(
                    ShortsSettings.hideShortsLocationLabel,
                    "yt_outline_location_point_",
                    "yt_outline_experimental_location_point_",
                ),
                ByteArrayFilterGroup(
                    ShortsSettings.hideShortsSaveSoundButton,
                    "yt_outline_bookmark_",
                    "yt_outline_list_add_",
                    "yt_outline_experimental_list_add_",
                ),
                ByteArrayFilterGroup(
                    ShortsSettings.hideShortsSearchSuggestions,
                    "yt_outline_search_",
                    "yt_outline_experimental_search_",
                ),
                ByteArrayFilterGroup(
                    ShortsSettings.hideShortsSuperThanksButton,
                    "yt_outline_dollar_sign_heart_",
                    "yt_outline_experimental_dollar_sign_heart_",
                ),
                ByteArrayFilterGroup(
                    ShortsSettings.hideShortsUseTemplateButton,
                    "yt_outline_template_add_",
                    "yt_outline_experimental_template_add_",
                ),
                ByteArrayFilterGroup(
                    ShortsSettings.hideShortsUpcomingButton,
                    "yt_outline_bell_",
                    "yt_outline_experimental_bell_",
                ),
                ByteArrayFilterGroup(
                    ShortsSettings.hideShortsEffectButton,
                    "/arcade/effects/icons/",
                ),
                ByteArrayFilterGroup(
                    ShortsSettings.hideShortsGreenScreenButton,
                    "greenscreen_temp",
                ),
                ByteArrayFilterGroup(
                    ShortsSettings.hideShortsNewPostsButton,
                    "yt_outline_box_pencil",
                    "yt_outline_experimental_box_pencil",
                ),
                ByteArrayFilterGroup(
                    ShortsSettings.hideShortsHashtagButton,
                    "yt_outline_hashtag_",
                    "yt_outline_experimental_hashtag_",
                ),
            )
        }

    val identifierGroups =
        arrayOf(
            FilterRule(null, "shorts_shelf", "inline_shorts", "shorts_grid", "shorts_video_cell") {
                shouldHideShortsFeedItems()
            },
            FilterRule(ShortsSettings.hideShortsChannel, "shorts_pivot_item"),
            FilterRule(null, "shelf_header.e") { it.index == 0 && shouldHideShortsFeedItems() },
        )

    val pathGroups =
        arrayOf(
            FilterRule(
                null,
                "compact_video.e",
                "video_lockup_with_attachment.e",
                "video_card.e",
            ) {
                shouldHideShortsFeedItems() && compactVideoBuffer.check(it.buffer).isFiltered
            },
            FilterRule(null, "shelf_header.e") {
                it.index == 0 &&
                    channelHeaderBuffer.check(it.buffer).isFiltered &&
                    shouldHideShortsFeedItems()
            },
            FilterRule(ShortsSettings.hideShortsJoinButton, "sponsor_button") {
                it.isPlayerOverlay
            },
            FilterRule(ShortsSettings.hideShortsSubscribeButton, "subscribe_button") {
                it.isPlayerOverlay
            },
            FilterRule(
                AdsSettings.hidePaidPromotionLabel,
                "reel_player_disclosure.e",
                "shorts_disclosures.e",
            ) {
                it.isPlayerOverlay
            },
            FilterRule(ShortsSettings.hideShortsLivePreview, "live_preview_page_vm.e"),
            FilterRule(null, "suggested_action.e") {
                suggestedActionsBuffer.all { group -> group.isEnabled } ||
                    suggestedActionsBuffer.check(it.buffer).isFiltered
            },
            FilterRule(ShortsSettings.hideShortsPausedOverlayButtons, "shorts_paused_state"),
            FilterRule(ShortsSettings.hideShortsChannelBar, "reel_channel_bar.e"),
            FilterRule(ShortsSettings.hideShortsInfoPanel, "shorts_info_panel_overview"),
            FilterRule(ShortsSettings.hideShortsPreviewComment, "participation_bar.e"),
            FilterRule(ShortsSettings.hideShortsAutoDubbedLabel, "badge.e") { it.isPlayerOverlay },
            FilterRule(ShortsSettings.hideShortsFullVideoLinkLabel, "reel_multi_format_link"),
            FilterRule(ShortsSettings.hideShortsVideoTitle, "shorts_video_title_item"),
            FilterRule(
                ShortsSettings.hideShortsUseSoundButton,
                "floating_action_button.e",
                "reel_metapanel.e",
            ) {
                useSoundBuffer.check(it.buffer).isFiltered
            },
            FilterRule(ShortsSettings.hideShortsUseTemplateButton, "reel_metapanel.e") {
                useTemplateBuffer.check(it.buffer).isFiltered
            },
            FilterRule(ShortsSettings.hideShortsSoundButton, "reel_pivot_button"),
            FilterRule(ShortsSettings.hideShortsStickers, "stickers_layer.e"),
            FilterRule(ShortsSettings.hideShortsTaggedProducts, "product_sticker.e"),
            FilterRule(null, "reel_carousel.e") { reelCarouselBuffer.check(it.buffer).isFiltered },
            FilterRule(ShortsSettings.hideShortsSoundMetadataLabel, "reel_sound_metadata"),
            FilterRule(ShortsSettings.hideShortsLikeFountain, "like_fountain.e"),
            FilterRule(
                ShortsSettings.hideShortsLikeButton,
                "shorts_like_button.e",
                "reel_like_button.e",
                "reel_like_toggled_button.e",
            ),
            FilterRule(
                ShortsSettings.hideShortsDislikeButton,
                "shorts_dislike_button.e",
                "reel_dislike_button.e",
                "reel_dislike_toggled_button.e",
            ),
            // The action bar's buttons share one component and keep their positions only while every button
            // shows; accessibility names each one.
            FilterRule(ShortsSettings.hideShortsCommentsButton, "reel_action_button.e") {
                it.accessibility.contains("id.reel_comment_button")
            },
            FilterRule(ShortsSettings.hideShortsShareButton, "reel_action_button.e") {
                it.accessibility.contains("id.reel_share_button")
            },
            FilterRule(ShortsSettings.hideShortsRemixButton, "reel_action_button.e") {
                it.accessibility.contains("id.reel_remix_button")
            },
        )
}

internal fun shouldHideShortsFeedItems(): Boolean {
    val hideHome = ShortsSettings.hideShortsHome.value
    val hideDescription = ShortsSettings.hideShortsVideoDescription.value
    if (!hideHome && !hideDescription) return false
    val descriptionOpen =
        PlayerType.current.isMaximizedOrFullscreen &&
            EngagementPanel.openPanelId == "video-description-ep-identifier"
    return if (descriptionOpen) hideDescription else hideHome
}
