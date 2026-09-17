// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.shorts;

import java.util.List;

import app.reseam.youtube.core.Logger;
import app.reseam.youtube.core.Settings;
import app.reseam.youtube.litho.Filter;
import app.reseam.youtube.litho.Filter.FilterContentType;
import app.reseam.youtube.litho.FilterGroup.ByteArrayFilterGroup;
import app.reseam.youtube.litho.FilterGroup.StringFilterGroup;
import app.reseam.youtube.litho.FilterGroupList.ByteArrayFilterGroupList;
import app.reseam.youtube.player.EngagementPanel;
import app.reseam.youtube.player.PlayerType;

/** Litho identifiers and paths used by the Shorts feed and player. */
public final class ShortsFilter extends Filter {
    private static final String COMPONENT_TYPE = "ComponentType";
    private static final String REEL_ACTION_BAR_PATH = "reel_action_bar.";
    private static final String REELS_PLAYER_OVERLAY_PATH = "reels_player_overlay_layout.";
    private static final String REEL_CHANNEL_BAR_PATH = "reel_channel_bar.e";
    private static final String REEL_METAPANEL_PATH = "reel_metapanel.e";
    private static final String REEL_PLAYER_OVERLAY_PATH = "reel_player_overlay.e";

    private final StringFilterGroup channelProfile;
    private final ByteArrayFilterGroup channelProfileShelfHeaderBuffer;
    private final StringFilterGroup useSoundButton;
    private final ByteArrayFilterGroup useSoundButtonBuffer;
    private final StringFilterGroup useTemplateButton;
    private final ByteArrayFilterGroup useTemplateButtonBuffer;
    private final StringFilterGroup reelCarousel;
    private final ByteArrayFilterGroupList reelCarouselBuffer = new ByteArrayFilterGroupList();
    private final StringFilterGroup suggestedAction;
    private final ByteArrayFilterGroupList suggestedActionsBuffer = new ByteArrayFilterGroupList();
    private final StringFilterGroup shortsCompactFeedVideo;
    private final ByteArrayFilterGroup shortsCompactFeedVideoBuffer;
    private final StringFilterGroup shelfHeaderIdentifier;
    private final StringFilterGroup shelfHeaderPath;
    private final StringFilterGroup joinButton;
    private final StringFilterGroup subscribeButton;
    private final StringFilterGroup paidPromotionLabel;
    private final StringFilterGroup autoDubbedLabel;
    private final StringFilterGroup likeFountain;

    public ShortsFilter() {
        final StringFilterGroup shortsIdentifiers = new StringFilterGroup(
                null, false, "shorts_shelf", "inline_shorts", "shorts_grid", "shorts_video_cell");
        channelProfile = new StringFilterGroup(
                "hide_shorts_channel", false, "shorts_pivot_item");
        channelProfileShelfHeaderBuffer = new ByteArrayFilterGroup(
                "hide_shorts_channel", false, "Shorts");
        shelfHeaderIdentifier = new StringFilterGroup(null, false, "shelf_header.e");
        addIdentifierCallbacks(shortsIdentifiers, channelProfile, shelfHeaderIdentifier);

        shortsCompactFeedVideo = new StringFilterGroup(
                null, false, "compact_video.e", "video_lockup_with_attachment.e", "video_card.e");
        shortsCompactFeedVideoBuffer = new ByteArrayFilterGroup(null, false, "/frame0.jpg");
        shelfHeaderPath = new StringFilterGroup(null, false, "shelf_header.e");

        final StringFilterGroup pausedOverlayButtons = new StringFilterGroup(
                "hide_shorts_paused_overlay_buttons", false, "shorts_paused_state");
        final StringFilterGroup channelBar = new StringFilterGroup(
                "hide_shorts_channel_bar", false, REEL_CHANNEL_BAR_PATH);
        final StringFilterGroup fullVideoLinkLabel = new StringFilterGroup(
                "hide_shorts_full_video_link_label", false, "reel_multi_format_link");
        final StringFilterGroup videoTitle = new StringFilterGroup(
                "hide_shorts_video_title", false, "shorts_video_title_item");
        final StringFilterGroup soundMetadata = new StringFilterGroup(
                "hide_shorts_sound_metadata_label", false, "reel_sound_metadata");
        final StringFilterGroup soundButton = new StringFilterGroup(
                "hide_shorts_sound_button", false, "reel_pivot_button");
        final StringFilterGroup infoPanel = new StringFilterGroup(
                "hide_shorts_info_panel", true, "shorts_info_panel_overview");
        final StringFilterGroup stickers = new StringFilterGroup(
                "hide_shorts_stickers", true, "stickers_layer.e");
        final StringFilterGroup likeButton = new StringFilterGroup(
                "hide_shorts_like_button", false,
                "shorts_like_button.e", "reel_like_button.e", "reel_like_toggled_button.e");
        likeFountain = new StringFilterGroup(
                "hide_shorts_like_fountain", true, "like_fountain.e");
        final StringFilterGroup dislikeButton = new StringFilterGroup(
                "hide_shorts_dislike_button", false,
                "shorts_dislike_button.e", "reel_dislike_button.e", "reel_dislike_toggled_button.e");
        final StringFilterGroup previewComment = new StringFilterGroup(
                "hide_shorts_preview_comment", true, "participation_bar.e");
        final StringFilterGroup livePreview = new StringFilterGroup(
                "hide_shorts_live_preview", false, "live_preview_page_vm.e");
        autoDubbedLabel = new StringFilterGroup(
                "hide_shorts_auto_dubbed_label", false, "badge.e");
        joinButton = new StringFilterGroup(
                "hide_shorts_join_button", true, "sponsor_button");
        subscribeButton = new StringFilterGroup(
                "hide_shorts_subscribe_button", true, "subscribe_button");
        paidPromotionLabel = new StringFilterGroup(
                "hide_paid_promotion_label", true, "reel_player_disclosure.e", "shorts_disclosures.e");

        reelCarousel = new StringFilterGroup(null, false, "reel_carousel.e");
        reelCarouselBuffer.addAll(
                new ByteArrayFilterGroup("hide_shorts_ai_button", false,
                        "yt_outline_info_circle", "yt_outline_experimental_info_circle"),
                new ByteArrayFilterGroup("hide_shorts_sound_metadata_label", false,
                        "yt_outline_audio", "yt_outline_experimental_audio"));

        useSoundButton = new StringFilterGroup(
                "hide_shorts_use_sound_button", true, "floating_action_button.e", REEL_METAPANEL_PATH);
        useSoundButtonBuffer = new ByteArrayFilterGroup(null, false, "yt_outline_camera_");
        useTemplateButton = new StringFilterGroup(
                "hide_shorts_use_template_button", true, REEL_METAPANEL_PATH);
        useTemplateButtonBuffer = new ByteArrayFilterGroup(null, false, "yt_outline_template_add_");

        suggestedAction = new StringFilterGroup(null, false, "suggested_action.e");
        suggestedActionsBuffer.addAll(
                new ByteArrayFilterGroup("hide_shorts_preview_comment", true, "shorts-comments-panel"),
                new ByteArrayFilterGroup("hide_shorts_shop_button", true,
                        "yt_outline_bag_", "yt_outline_experimental_bag_"),
                new ByteArrayFilterGroup("hide_shorts_tagged_products", true, "PAproduct_listZ"),
                new ByteArrayFilterGroup("hide_shorts_location_label", false,
                        "yt_outline_location_point_", "yt_outline_experimental_location_point_"),
                new ByteArrayFilterGroup("hide_shorts_save_sound_button", true,
                        "yt_outline_bookmark_", "yt_outline_list_add_", "yt_outline_experimental_list_add_"),
                new ByteArrayFilterGroup("hide_shorts_search_suggestions", true,
                        "yt_outline_search_", "yt_outline_experimental_search_"),
                new ByteArrayFilterGroup("hide_shorts_super_thanks_button", true,
                        "yt_outline_dollar_sign_heart_", "yt_outline_experimental_dollar_sign_heart_"),
                new ByteArrayFilterGroup("hide_shorts_use_template_button", true,
                        "yt_outline_template_add_", "yt_outline_experimental_template_add_"),
                new ByteArrayFilterGroup("hide_shorts_upcoming_button", true,
                        "yt_outline_bell_", "yt_outline_experimental_bell_"),
                new ByteArrayFilterGroup("hide_shorts_effect_button", true, "/arcade/effects/icons/"),
                new ByteArrayFilterGroup("hide_shorts_green_screen_button", true, "greenscreen_temp"),
                new ByteArrayFilterGroup("hide_shorts_new_posts_button", true,
                        "yt_outline_box_pencil", "yt_outline_experimental_box_pencil"),
                new ByteArrayFilterGroup("hide_shorts_hashtag_button", true,
                        "yt_outline_hashtag_", "yt_outline_experimental_hashtag_"));

        addPathCallbacks(
                shortsCompactFeedVideo, shelfHeaderPath, joinButton, subscribeButton,
                paidPromotionLabel, livePreview, suggestedAction, pausedOverlayButtons, channelBar,
                infoPanel, previewComment, autoDubbedLabel, fullVideoLinkLabel, videoTitle,
                useSoundButton, useTemplateButton, soundButton, stickers, reelCarousel, soundMetadata,
                likeFountain, likeButton, dislikeButton);
    }

    @Override
    public boolean isFiltered(String identifier, String accessibility, String path, byte[] buffer,
                              StringFilterGroup matchedGroup, FilterContentType contentType,
                              int contentIndex) {
        if (contentType == FilterContentType.IDENTIFIER) {
            if (matchedGroup == shelfHeaderIdentifier && contentIndex != 0) return false;
            if (matchedGroup == channelProfile) return filtered(true, contentType, identifier, path);
            return filtered(shouldHideShortsFeedItems(), contentType, identifier, path);
        }

        if (contentType != FilterContentType.PATH) return false;
        if (matchedGroup == joinButton || matchedGroup == subscribeButton
                || matchedGroup == paidPromotionLabel || matchedGroup == autoDubbedLabel) {
            return filtered(path.startsWith(REEL_CHANNEL_BAR_PATH) || path.startsWith(REEL_METAPANEL_PATH)
                    || path.startsWith(REEL_PLAYER_OVERLAY_PATH), contentType, identifier, path);
        }
        if (matchedGroup == reelCarousel) {
            return filtered(reelCarouselBuffer.check(buffer).isFiltered(), contentType, identifier, path);
        }
        if (matchedGroup == useSoundButton) {
            return filtered(useSoundButtonBuffer.check(buffer).isFiltered(), contentType, identifier, path);
        }
        if (matchedGroup == useTemplateButton) {
            return filtered(useTemplateButtonBuffer.check(buffer).isFiltered(), contentType, identifier, path);
        }
        if (matchedGroup == shortsCompactFeedVideo) {
            return filtered(shouldHideShortsFeedItems()
                    && shortsCompactFeedVideoBuffer.check(buffer).isFiltered(), contentType, identifier, path);
        }
        if (matchedGroup == shelfHeaderPath) {
            return filtered(contentIndex == 0 && channelProfileShelfHeaderBuffer.check(buffer).isFiltered()
                    && shouldHideShortsFeedItems(), contentType, identifier, path);
        }
        if (matchedGroup == suggestedAction) {
            return filtered(allSuggestedActionsHidden()
                    || suggestedActionsBuffer.check(buffer).isFiltered(), contentType, identifier, path);
        }
        return filtered(true, contentType, identifier, path);
    }

    private static boolean filtered(boolean result, FilterContentType contentType,
                                    String identifier, String path) {
        if (result) {
            Logger.debug(() -> "ShortsFilter filtered " + contentType
                    + " identifier=" + identifier + " path=" + path);
        }
        return result;
    }

    private boolean allSuggestedActionsHidden() {
        for (ByteArrayFilterGroup group : suggestedActionsBuffer) {
            if (!group.isEnabled()) return false;
        }
        return true;
    }

    private boolean shouldHideShortsFeedItems() {
        final boolean hideHome = Settings.getBoolean("hide_shorts_home", false);
        final boolean hideVideoDescription = Settings.getBoolean("hide_shorts_video_description", false);
        if (!hideHome && !hideVideoDescription) return false;
        if (PlayerType.current().isMaximizedOrFullscreen()) {
            return EngagementPanel.openPanelId().equals("video-description-ep-identifier")
                    ? hideVideoDescription : hideHome;
        }
        return hideHome;
    }

    /** Called from the 20.22+ component-context method for the Shorts action-bar list. */
    public static void hideActionButtons(StringBuilder pathBuilder, List<Object> results) {
        try {
            Logger.debug(() -> "ShortsFilter action buttons hook fired");
            if (pathBuilder == null || pathBuilder.length() == 0 || results == null || results.size() < 4
                    || (!pathBuilder.toString().contains(REEL_ACTION_BAR_PATH)
                    && !pathBuilder.toString().contains(REELS_PLAYER_OVERLAY_PATH))) return;
            if (results.get(0) == null || !COMPONENT_TYPE.equals(results.get(0).toString())) return;
            for (int i = results.size() - 1; i >= 0; i--) {
                if (results.get(i) == null) continue;
                final boolean hide = (i == 2 && Settings.getBoolean("hide_shorts_comments_button", false))
                        || (i == 3 && Settings.getBoolean("hide_shorts_share_button", false))
                        || (i == 4 && Settings.getBoolean("hide_shorts_remix_button", false));
                if (hide) {
                    final int index = i;
                    Logger.debug(() -> "ShortsFilter filtered action button index " + index);
                    results.remove(i);
                }
            }
        } catch (Exception exception) {
            Logger.error(() -> "ShortsFilter action button filtering failed: " + exception);
        }
    }

}
