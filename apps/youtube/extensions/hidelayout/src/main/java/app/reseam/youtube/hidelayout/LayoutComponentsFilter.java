// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.hidelayout;

import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import app.reseam.youtube.core.Logger;
import app.reseam.youtube.core.Settings;
import app.reseam.youtube.litho.Filter;
import app.reseam.youtube.litho.FilterGroup.ByteArrayFilterGroup;
import app.reseam.youtube.litho.FilterGroup.StringFilterGroup;
import app.reseam.youtube.litho.FilterGroupList.StringFilterGroupList;
import app.reseam.youtube.litho.ByteTrieSearch;
import app.reseam.youtube.litho.StringTrieSearch;
import app.reseam.youtube.navigation.NavigationBar;
import app.reseam.youtube.player.PlayerType;

/** Runtime gates for the general layout-component hiding patch. */
public final class LayoutComponentsFilter extends Filter {
    /** Bridge implemented on YouTube's obfuscated search-suggestion value class by the patch. */
    public interface SearchSuggestionAccessor {
        boolean patch_isSearchHistory();
    }

    private static final String[] MIX_CONTEXT_EXCEPTIONS = {"V.ED", "java.lang.ref.WeakReference"};
    private static final ByteArrayFilterGroup MIX_BUFFER_EXCEPTIONS = new ByteArrayFilterGroup(
            null, false, "cell_description_body", "channel_profile");
    private static final ByteArrayFilterGroup MIX_PLAYLISTS = new ByteArrayFilterGroup(
            null, false, "&list=");

    private final StringTrieSearch exceptions = new StringTrieSearch();
    private final StringFilterGroup communityPosts;
    private final StringFilterGroup surveys;
    private final StringFilterGroup notifyMe;
    private final StringFilterGroup singleItemInformationPanel;
    private final StringFilterGroup expandableMetadata;
    private final StringFilterGroup compactChannelBarInner;
    private final StringFilterGroup innerButton;
    private final ByteArrayFilterGroup joinMembershipButton;
    private final StringFilterGroup chipBar;
    private final StringFilterGroup channelProfile;
    private final StringFilterGroupList channelProfileGroups = new StringFilterGroupList();
    private final StringFilterGroup horizontalShelves;
    private final StringFilterGroup movieSections;
    private final ByteArrayFilterGroup playablesBuffer;
    private final ByteArrayFilterGroup ticketShelfBuffer;
    private final ByteArrayFilterGroup creatorStoreBuffer;
    private final ByteArrayFilterGroup buyMovieBuffer;
    private final ByteTrieSearch descriptionSearch = new ByteTrieSearch();

    public LayoutComponentsFilter() {
        for (String exception : new String[]{
                "home_video_with_context", "related_video_with_context", "search_video_with_context",
                "comment_thread", "|comment.", "library_recent_shelf"}) {
            exceptions.addPattern(exception);
        }

        StringFilterGroup chips = new StringFilterGroup("hide_chips_shelf", true, "chips_shelf");
        StringFilterGroup audioButton = new StringFilterGroup(null, false, "multi_feed_icon_button");
        addIdentifierCallbacks(
                new StringFilterGroup("hide_compact_banner", true, "cell_divider"),
                chips,
                new StringFilterGroup("hide_live_chat_replay_button", false, "live_chat_ep_entrypoint.e"),
                new StringFilterGroup("hide_visual_spacer", true, "cell_divider"));

        communityPosts = new StringFilterGroup("hide_community_posts", false,
                "post_base_wrapper", "text_post_root.e", "images_post_root.e", "images_post_slim.e",
                "images_post_root_slim.e", "text_post_root_slim.e", "post_base_wrapper_slim.e",
                "poll_post_root.e", "videos_post_root.e", "post_shelf_slim.e",
                "videos_post_responsive_root.e", "text_post_responsive_root.e",
                "poll_post_responsive_root.e", "shared_post_root.e");
        StringFilterGroup subscribersGuidelines = new StringFilterGroup(
                "hide_subscribers_community_guidelines", true, "sponsorships_comments_upsell");
        StringFilterGroup membersShelf = new StringFilterGroup("hide_members_shelf", true, "member_recognition_shelf");
        StringFilterGroup compactBanner = new StringFilterGroup("hide_compact_banner", true, "compact_banner");
        StringFilterGroup subscriptionsChipBar = new StringFilterGroup(
                "hide_filter_bar_feed_in_feed", false, "subscriptions_chip_bar");
        StringFilterGroup subscribedChannels = new StringFilterGroup(
                "hide_subscribed_channels_bar", false, "subscriptions_channel_bar");
        chipBar = new StringFilterGroup("hide_filter_bar_feed_in_history", false, "chip_bar");
        surveys = new StringFilterGroup("hide_surveys", true, "in_feed_survey", "slimline_survey", "feed_nudge");
        StringFilterGroup medical = new StringFilterGroup("hide_medical_panels", true, "medical_panel");
        StringFilterGroup paidPromotion = new StringFilterGroup("hide_paid_promotion_label", true, "paid_content_overlay");
        StringFilterGroup infoPanel = new StringFilterGroup("hide_info_panels", true, "publisher_transparency_panel");
        singleItemInformationPanel = new StringFilterGroup(
                "hide_info_panels", true, "single_item_information_panel");
        StringFilterGroup latestPosts = new StringFilterGroup("hide_latest_posts", true, "post_shelf");
        StringFilterGroup links = new StringFilterGroup("hide_links_preview", true, "attribution.e");
        StringFilterGroup emergency = new StringFilterGroup("hide_emergency_box", true, "emergency_onebox");
        StringFilterGroup artistCard = new StringFilterGroup("hide_artist_cards", false, "official_card");
        expandableMetadata = new StringFilterGroup("hide_expandable_card", true, "inline_expander");
        StringFilterGroup compactChannelBar = new StringFilterGroup("hide_channel_bar", false, "compact_channel_bar");
        compactChannelBarInner = new StringFilterGroup("hide_join_membership_button", true, "compact_channel_bar_inner");
        innerButton = new StringFilterGroup(null, false, "|button.e");
        joinMembershipButton = new ByteArrayFilterGroup(null, false, "sponsorships");
        StringFilterGroup imageShelf = new StringFilterGroup("hide_image_shelf", true, "image_shelf");
        StringFilterGroup crowdfunding = new StringFilterGroup("hide_crowdfunding_box", false, "donation_shelf");
        StringFilterGroup watermark = new StringFilterGroup("hide_channel_watermark", true, "featured_channel_watermark_overlay");
        StringFilterGroup forYou = new StringFilterGroup("hide_for_you_shelf", false, "mixed_content_shelf");
        StringFilterGroup recommendationLabels = new StringFilterGroup(
                "hide_video_recommendation_labels", true, "endorsement_header_footer.e");
        StringFilterGroup videoTitle = new StringFilterGroup("hide_video_title", false, "player_overlay_video_heading.e");
        StringFilterGroup webResults = new StringFilterGroup(
                "hide_web_search_results", true, "web_link_panel", "web_result_panel");
        movieSections = new StringFilterGroup(
                "hide_movies_section", true, "browsy_bar", "compact_movie", "compact_tvfilm_item",
                "horizontal_movie_shelf", "movie_and_show_upsell_card", "offer_module_root",
                "video_lockup_with_attachment.e");
        channelProfile = new StringFilterGroup(null, false, "channel_profile.e", "page_header.e");
        channelProfileGroups.addAll(
                new StringFilterGroup("hide_community_button", true, "community_button"),
                new StringFilterGroup("hide_join_button", false, "sponsor_button"),
                new StringFilterGroup("hide_store_button", true, "header_store_button"),
                new StringFilterGroup("hide_subscribe_button_in_channel_page", false, "subscribe_button"));
        horizontalShelves = new StringFilterGroup(null, false,
                "horizontal_video_shelf.e", "horizontal_shelf.e", "horizontal_shelf_inline.e",
                "horizontal_tile_shelf.e");
        ticketShelfBuffer = new ByteArrayFilterGroup(null, false, "ticket_item.e");
        playablesBuffer = new ByteArrayFilterGroup(null, false, "FEmini_app_destination");
        creatorStoreBuffer = new ByteArrayFilterGroup(null, false, "shopping_item_card_list");
        buyMovieBuffer = new ByteArrayFilterGroup(null, false, "FEstorefront");

        addDescriptionSearch("hide_featured_places_section", "yt_fill_star");
        addDescriptionSearch("hide_featured_places_section", "yt_fill_experimental_star");
        addDescriptionSearch("hide_gaming_section", "yt_outline_gaming");
        addDescriptionSearch("hide_gaming_section", "yt_outline_experimental_gaming");
        addDescriptionSearch("hide_music_section", "yt_outline_audio");
        addDescriptionSearch("hide_music_section", "yt_outline_experimental_audio");
        addDescriptionSearch("hide_quizzes_section", "post_base_wrapper_slim");
        addDescriptionSearch("hide_attributes_section", "cell_video_attribute");

        StringFilterGroup notifyMeGroup = new StringFilterGroup(
                "hide_notify_me_button", true, "set_reminder_button");
        notifyMe = notifyMeGroup;
        addPathCallbacks(artistCard, audioButton, links, membersShelf, channelProfile, watermark,
                chipBar, compactBanner, compactChannelBar, compactChannelBarInner, communityPosts,
                crowdfunding, emergency, expandableMetadata, forYou, horizontalShelves, chips,
                imageShelf, infoPanel, latestPosts, medical, movieSections, notifyMeGroup, paidPromotion,
                new StringFilterGroup("hide_playables", true, "horizontal_gaming_shelf.e", "mini_game_card.e"),
                new StringFilterGroup("hide_quick_actions", false, "quick_actions"),
                new StringFilterGroup("hide_related_videos", false, "fullscreen_related_videos"),
                singleItemInformationPanel, subscribedChannels, subscribersGuidelines, subscriptionsChipBar,
                surveys, new StringFilterGroup("hide_timed_reactions", true, "emoji_control_panel", "timed_reaction"),
                videoTitle, recommendationLabels, webResults);
    }

    private void addDescriptionSearch(String setting, String pattern) {
        descriptionSearch.addPattern(pattern.getBytes(StandardCharsets.UTF_8),
                (text, start, length, result) -> {
                    ((AtomicReference<Boolean>) result).set(Settings.getBoolean(setting, false));
                    return true;
                });
    }

    private static List<String> readLines(String key) {
        List<String> result = new ArrayList<>();
        for (String line : Settings.getString(key, "").split("\\n")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) result.add(trimmed);
        }
        return result;
    }

    @Override
    public boolean isFiltered(String identifier, String accessibility, String path, byte[] buffer,
                              StringFilterGroup matchedGroup, FilterContentType contentType, int contentIndex) {
        if (matchedGroup == singleItemInformationPanel) {
            return PlayerType.current().isMaximizedOrFullscreen() || !NavigationBar.isSearchBarActive();
        }
        if (matchedGroup == notifyMe || matchedGroup == surveys || matchedGroup == expandableMetadata) return true;
        if (matchedGroup == movieSections) {
            return !path.contains("video_lockup_with_attachment.e") || buyMovieBuffer.check(buffer).isFiltered();
        }
        if (matchedGroup == channelProfile) return channelProfileGroups.check(accessibility).isFiltered();
        if (matchedGroup == communityPosts && NavigationBar.isBackButtonVisible()
                && !NavigationBar.isSearchBarActive() && PlayerType.current() != PlayerType.WATCH_WHILE_MAXIMIZED) {
            return false;
        }
        if (exceptions.matches(path)) return false;
        if (matchedGroup == compactChannelBarInner) {
            return innerButton.check(path).isFiltered() && joinMembershipButton.check(buffer).isFiltered();
        }
        if (matchedGroup == horizontalShelves) {
            if (contentIndex != 0) return false;
            AtomicReference<Boolean> descriptionResult = new AtomicReference<>();
            if (descriptionSearch.matches(buffer, descriptionResult)) return Boolean.TRUE.equals(descriptionResult.get());
            boolean hideShelves = Settings.getBoolean("hide_horizontal_shelves", true);
            boolean hideTickets = Settings.getBoolean("hide_ticket_shelf", false);
            boolean hidePlayables = Settings.getBoolean("hide_playables", true);
            boolean hideStore = Settings.getBoolean("hide_creator_store_shelf", true);
            if (!hideShelves && !hideTickets && !hidePlayables && !hideStore) return false;
            if (ticketShelfBuffer.check(buffer).isFiltered()) return hideTickets;
            if (playablesBuffer.check(buffer).isFiltered()) return hidePlayables;
            if (creatorStoreBuffer.check(buffer).isFiltered()) return hideStore;
            PlayerType type = PlayerType.current();
            if (hideStore && (type == PlayerType.WATCH_WHILE_MAXIMIZED || type == PlayerType.WATCH_WHILE_FULLSCREEN)) return true;
            return hideShelves && hideShelves();
        }
        if (matchedGroup == chipBar) {
            return contentIndex == 0
                    && NavigationBar.NavigationButton.getSelectedNavigationButton()
                    == NavigationBar.NavigationButton.LIBRARY;
        }
        return true;
    }

    private static boolean hideShelves() {
        if (PlayerType.current().isMaximizedOrFullscreen()) return false;
        if (NavigationBar.isSearchBarActive()) return true;
        if (NavigationBar.isBackButtonVisible()) return false;
        return NavigationBar.NavigationButton.getSelectedNavigationButton()
                != NavigationBar.NavigationButton.LIBRARY;
    }

    public static boolean filterMixPlaylists(Object conversionContext, byte[] buffer) {
        if (!Settings.getBoolean("hide_mix_playlists", true) || buffer == null) return false;
        if (!MIX_PLAYLISTS.check(buffer).isFiltered() || MIX_BUFFER_EXCEPTIONS.check(buffer).isFiltered()) return false;
        String context = String.valueOf(conversionContext);
        for (String exception : MIX_CONTEXT_EXCEPTIONS) if (context.contains(exception)) return false;
        Logger.debug(() -> "LayoutComponentsFilter filtered mix playlist");
        return true;
    }


    public static void hideAlbumCard(View view) { hide(view, "hide_album_cards"); }
    public static void hideCrowdfundingBox(View view) { hide(view, "hide_crowdfunding_box"); }
    public static void hideLatestVideosButton(View view) { hideView(view, "hide_latest_videos_button"); }
    public static void hideSubscribedChannelsBar(View view) { hide(view, "hide_subscribed_channels_bar"); }
    public static void hideInRelatedVideos(View view) { hideView(view, "hide_filter_bar_feed_in_related_videos"); }


    public static void hideShowMoreButton(View root, View buttonContainer, TextView textView) {
        if (!Settings.getBoolean("hide_show_more_button", true) || root == null || textView == null) return;
        if (TextUtils.equals(root.getContentDescription(), textView.getText())) {
            hide(root, null);
            if (buttonContainer != null) hide(buttonContainer, null);
        }
    }

    public static CharSequence modifyFeedSubtitleSpan(CharSequence original, float dimension) {
        if (original == null || (!Settings.getBoolean("hide_view_count", false) && !Settings.getBoolean("hide_upload_time", false))
                || (dimension != 16f && dimension != 42f)) return original;
        String delimiter = " · ";
        int viewStart = TextUtils.indexOf(original, delimiter);
        int uploadStart = viewStart < 0 ? -1 : TextUtils.indexOf(original, delimiter, viewStart + delimiter.length());
        if (uploadStart < 0 || TextUtils.indexOf(original, delimiter, uploadStart + delimiter.length()) >= 0) return original;
        SpannableStringBuilder builder = new SpannableStringBuilder(original);
        if (Settings.getBoolean("hide_upload_time", false)) builder.delete(uploadStart, builder.length());
        if (Settings.getBoolean("hide_view_count", false)) builder.delete(viewStart, uploadStart);
        SpannableString replacement = new SpannableString(builder);
        Logger.debug(() -> "LayoutComponentsFilter modified subtitle: " + original);
        return replacement;
    }

    public static CharSequence hideFlyoutMenu(CharSequence title) {
        if (title == null || !Settings.getBoolean("hide_feed_flyout_menu", false)) return title;
        for (String filter : readLines("hide_feed_flyout_menu_filter_strings")) {
            if (title.toString().equalsIgnoreCase(filter)) {
                Logger.debug(() -> "LayoutComponentsFilter hid flyout menu: " + title);
                return null;
            }
        }
        return title;
    }

    public static void hideFlyoutMenu(TextView textView, CharSequence title) {
        if (title == null || !Settings.getBoolean("hide_feed_flyout_menu", false) || textView == null) return;
        for (String filter : readLines("hide_feed_flyout_menu_filter_strings")) {
            if (title.toString().equalsIgnoreCase(filter)) {
                if (textView.getParent() instanceof View) hide((View) textView.getParent(), null);
                Logger.debug(() -> "LayoutComponentsFilter hid flyout menu: " + title);
                return;
            }
        }
    }

    /** The patch keeps the tab view but hides it; this is safe at the UI seam and preserves the list. */
    public static void hideChannelTabView(View tabView, String tabText) {
        if (tabText == null || !Settings.getBoolean("hide_channel_tab", false)) return;
        for (String filter : readLines("hide_channel_tab_filter_strings")) {
            if (tabText.equalsIgnoreCase(filter)) {
                hide(tabView, null);
                Logger.debug(() -> "LayoutComponentsFilter hid channel tab: " + tabText);
                return;
            }
        }
    }

    public static boolean hideYouMayLikeSection(String typedString) {
        boolean hidden = Settings.getBoolean("hide_you_may_like_section", true)
                && TextUtils.isEmpty(typedString);
        if (hidden) Logger.debug(() -> "LayoutComponentsFilter hid You may like section");
        return hidden;
    }

    public static boolean isSearchHistory(Object searchTerm, String endpoint) {
        boolean history = endpoint != null && endpoint.contains("/delete");
        if (!history) Logger.debug(() -> "LayoutComponentsFilter removed search suggestion: " + searchTerm);
        return history;
    }

    public static void filterSearchSuggestions(Collection<?> suggestions) {
        if (suggestions == null) return;
        Iterator<?> iterator = suggestions.iterator();
        while (iterator.hasNext()) {
            Object suggestion = iterator.next();
            if (suggestion instanceof SearchSuggestionAccessor
                    && !((SearchSuggestionAccessor) suggestion).patch_isSearchHistory()) {
                iterator.remove();
            }
        }
    }

    private static void hide(View view, String setting) {
        if (view == null || (setting != null && !Settings.getBoolean(setting, false))) return;
        view.setVisibility(View.GONE);
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params != null && setting != null) {
            params.width = 0;
            params.height = 0;
            view.setLayoutParams(params);
        }
        Logger.debug(() -> "LayoutComponentsFilter filtered view: " + view.getClass().getSimpleName());
    }

    private static void hideView(View view, String setting) {
        if (view == null || !Settings.getBoolean(setting, false)) return;
        view.setVisibility(View.GONE);
        Logger.debug(() -> "LayoutComponentsFilter filtered view: " + view.getClass().getSimpleName());
    }
}
