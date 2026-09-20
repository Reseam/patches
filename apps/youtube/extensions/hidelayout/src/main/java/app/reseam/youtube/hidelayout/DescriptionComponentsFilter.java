// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.hidelayout;

import app.reseam.youtube.core.Logger;
import app.reseam.youtube.core.Settings;
import app.reseam.youtube.litho.Filter;
import app.reseam.youtube.litho.FilterGroup.ByteArrayFilterGroup;
import app.reseam.youtube.litho.FilterGroup.StringFilterGroup;
import app.reseam.youtube.litho.FilterGroupList.ByteArrayFilterGroupList;
import app.reseam.youtube.player.EngagementPanel;
import app.reseam.youtube.player.PlayerType;

/** Filters the sections that YouTube puts in a video's description panel. */
public final class DescriptionComponentsFilter extends Filter {
    private static final String INFOCARDS_PATH = "infocards_section.e";

    private final StringFilterGroup playlistSection;
    private final ByteArrayFilterGroupList playlistBuffers = new ByteArrayFilterGroupList();
    private final StringFilterGroup macroMarkers;
    private final ByteArrayFilterGroupList macroBuffers = new ByteArrayFilterGroupList();
    private final StringFilterGroup featuredLinks;
    private final StringFilterGroup featuredVideos;
    private final StringFilterGroup subscribeButton;

    public DescriptionComponentsFilter() {
        StringFilterGroup aiSummary = new StringFilterGroup(
                "hide_ai_generated_video_summary_section", false, "cell_expandable_metadata.e");
        // The Ask entry point is the description panel's composer button on this release.
        StringFilterGroup ask = new StringFilterGroup("hide_ask_section", false, "input_composer_button.e");
        StringFilterGroup attributes = new StringFilterGroup(
                "hide_attributes_section", false, "video_attributes_section");
        featuredLinks = new StringFilterGroup("hide_featured_links_section", false, "media_lockup");
        featuredVideos = new StringFilterGroup(
                "hide_featured_videos_section", false, "structured_description_video_lockup");

        playlistSection = new StringFilterGroup(null, false, "playlist_section.e");
        playlistBuffers.addAll(
                new ByteArrayFilterGroup("hide_explore_course_section", false,
                        "yt_outline_creator_academy", "yt_outline_experimental_graduation_cap"),
                new ByteArrayFilterGroup("hide_explore_podcast_section", false, "FEpodcasts_destination"));

        StringFilterGroup transcript = new StringFilterGroup(
                "hide_transcript_section", true, "transcript_section");
        StringFilterGroup howMade = new StringFilterGroup(
                "hide_how_this_was_made_section", false, "how_this_was_made_section");
        StringFilterGroup courseProgress = new StringFilterGroup(
                "hide_course_progress_section", false, "course_progress");
        StringFilterGroup hype = new StringFilterGroup("hide_hype_points", false, "hype_points_factoid");
        StringFilterGroup infoCards = new StringFilterGroup(
                "hide_info_cards_section", true, INFOCARDS_PATH);
        subscribeButton = new StringFilterGroup("hide_subscribe_button", false, "subscribe_button");

        macroMarkers = new StringFilterGroup(null, false, "macro_markers_carousel.e");
        macroBuffers.addAll(
                new ByteArrayFilterGroup("hide_chapters_section", true,
                        "chapters_horizontal_shelf", "auto-chapters", "description-chapters"),
                new ByteArrayFilterGroup("hide_key_concepts_section", false,
                        "learning_concept_macro_markers_carousel_shelf", "learning-concept"));

        addPathCallbacks(aiSummary, ask, attributes, courseProgress, featuredLinks, featuredVideos,
                howMade, hype, infoCards, macroMarkers, playlistSection, subscribeButton, transcript);
    }

    @Override
    public boolean isFiltered(String identifier, String accessibility, String path, byte[] buffer,
                              StringFilterGroup matchedGroup, FilterContentType contentType, int contentIndex) {
        String panel = EngagementPanel.openPanelId();
        if (!(panel.contains("description") || panel.contains("watch_description"))
                || PlayerType.current() == PlayerType.WATCH_WHILE_MINIMIZED) {
            return false;
        }

        if (matchedGroup == featuredLinks || matchedGroup == featuredVideos || matchedGroup == subscribeButton) {
            boolean filtered = path.startsWith(INFOCARDS_PATH);
            if (filtered) Logger.debug(() -> "DescriptionComponentsFilter filtered path: " + path);
            return filtered;
        }
        if (matchedGroup == playlistSection) {
            boolean filtered = contentIndex == 0
                    && (Settings.getBoolean("hide_explore_section", true)
                    || playlistBuffers.check(buffer).isFiltered());
            if (filtered) Logger.debug(() -> "DescriptionComponentsFilter filtered path: " + path);
            return filtered;
        }
        if (matchedGroup == macroMarkers) {
            boolean filtered = contentIndex == 0 && macroBuffers.check(buffer).isFiltered();
            if (filtered) Logger.debug(() -> "DescriptionComponentsFilter filtered path: " + path);
            return filtered;
        }
        Logger.debug(() -> "DescriptionComponentsFilter filtered path: " + path);
        return true;
    }
}
