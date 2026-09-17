// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.hidelayout;

import app.reseam.youtube.core.Logger;
import app.reseam.youtube.litho.Filter;
import app.reseam.youtube.litho.FilterGroup.ByteArrayFilterGroup;
import app.reseam.youtube.litho.FilterGroup.StringFilterGroup;
import app.reseam.youtube.player.PlayerType;

/** Filters comments and the controls embedded in the comments composer. */
public final class CommentsFilter extends Filter {
    private static final String COMMENT_COMPOSER_PATH = "comment_composer.e";
    private static final String VIDEO_LOCKUP_PATH = "video_lockup_with_attachment.e";

    private final StringFilterGroup aiSummary;
    private final ByteArrayFilterGroup aiSummaryIcon;
    private final StringFilterGroup comments;
    private final StringFilterGroup composerButtons;

    public CommentsFilter() {
        StringFilterGroup chatSummary = new StringFilterGroup(
                "hide_comments_ai_chat_summary", false, "live_chat_summary_banner.e");
        aiSummary = new StringFilterGroup("hide_comments_ai_summary", false, "chip_bar.e");
        aiSummaryIcon = new ByteArrayFilterGroup(null, false, "yt_fill_spark_");
        StringFilterGroup channelGuidelines = new StringFilterGroup(
                "hide_comments_channel_guidelines", true, "channel_guidelines_entry_banner");
        StringFilterGroup members = new StringFilterGroup(
                "hide_comments_by_members_header", false,
                "sponsorships_comments_header.e", "sponsorships_comments_footer.e");
        // The callback chooses the home-feed or player toggle independently.
        comments = new StringFilterGroup(null, false,
                "video_metadata_carousel", "_comments");
        StringFilterGroup communityGuidelines = new StringFilterGroup(
                "hide_comments_community_guidelines", true, "community_guidelines");
        StringFilterGroup createShort = new StringFilterGroup(
                "hide_comments_create_a_short_button", true, "composer_short_creation_button.e");
        composerButtons = new StringFilterGroup(
                "hide_comments_emoji_and_timestamp_buttons", false,
                "|CellType|ContainerType|ContainerType|ContainerType|ContainerType|ContainerType|");
        StringFilterGroup preview = new StringFilterGroup(
                "hide_comments_preview_comment", false,
                "|carousel_item", "comments_entry_point_teaser", "comments_entry_point_simplebox");
        StringFilterGroup thanks = new StringFilterGroup(
                "hide_comments_thanks_button", true, "super_thanks_button.e");
        addPathCallbacks(channelGuidelines, chatSummary, aiSummary, members, comments,
                communityGuidelines, createShort, composerButtons, preview, thanks);
    }

    @Override
    public boolean isFiltered(String identifier, String accessibility, String path, byte[] buffer,
                              StringFilterGroup matchedGroup, FilterContentType contentType, int contentIndex) {
        if (matchedGroup == aiSummary) {
            boolean filtered = PlayerType.current().isMaximizedOrFullscreen() && aiSummaryIcon.check(buffer).isFiltered();
            if (filtered) Logger.debug(() -> "CommentsFilter filtered path: " + path);
            return filtered;
        }
        if (matchedGroup == comments) {
            boolean filtered = path.startsWith(VIDEO_LOCKUP_PATH)
                    ? app.reseam.youtube.core.Settings.getBoolean("hide_comments_section_in_home_feed", false)
                    : app.reseam.youtube.core.Settings.getBoolean("hide_comments_section", false);
            if (filtered) Logger.debug(() -> "CommentsFilter filtered path: " + path);
            return filtered;
        }
        if (matchedGroup == composerButtons) {
            boolean filtered = path.startsWith(COMMENT_COMPOSER_PATH);
            if (filtered) Logger.debug(() -> "CommentsFilter filtered path: " + path);
            return filtered;
        }
        Logger.debug(() -> "CommentsFilter filtered path: " + path);
        return true;
    }
}
