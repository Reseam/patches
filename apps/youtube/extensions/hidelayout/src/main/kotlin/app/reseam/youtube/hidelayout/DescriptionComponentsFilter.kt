// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.hidelayout

import app.reseam.runtime.Export
import app.reseam.youtube.litho.Filter
import app.reseam.youtube.litho.FilterGroup.ByteArrayFilterGroup
import app.reseam.youtube.litho.FilterGroup.StringFilterGroup
import app.reseam.youtube.litho.FilterGroupList.ByteArrayFilterGroupList
import app.reseam.youtube.litho.FilterRule
import app.reseam.youtube.litho.LithoComponent
import app.reseam.youtube.player.EngagementPanel
import app.reseam.youtube.player.PlayerType
import app.reseam.youtube.settings.DescriptionSettings

@Export
class DescriptionComponentsFilter : Filter() {
    private val playlistBuffers =
        ByteArrayFilterGroupList().apply {
            addAll(
                ByteArrayFilterGroup(
                    DescriptionSettings.hideExploreCourseSection,
                    "yt_outline_creator_academy",
                    "yt_outline_experimental_graduation_cap",
                ),
                ByteArrayFilterGroup(
                    DescriptionSettings.hideExplorePodcastSection,
                    "FEpodcasts_destination",
                ),
            )
        }
    private val macroBuffers =
        ByteArrayFilterGroupList().apply {
            addAll(
                ByteArrayFilterGroup(
                    DescriptionSettings.hideChaptersSection,
                    "chapters_horizontal_shelf",
                    "auto-chapters",
                    "description-chapters",
                ),
                ByteArrayFilterGroup(
                    DescriptionSettings.hideKeyConceptsSection,
                    "learning_concept_macro_markers_carousel_shelf",
                    "learning-concept",
                ),
            )
        }
    private val cardBuffers =
        ByteArrayFilterGroupList().apply {
            addAll(
                ByteArrayFilterGroup(DescriptionSettings.hideMusicSection, "music_album_hero_card")
            )
        }

    init {
        addPathCallbacks(
            FilterRule(
                DescriptionSettings.hideAiGeneratedVideoSummarySection,
                "cell_expandable_metadata.e",
            ),
            // Newer releases render the music section as an album hero card.
            FilterRule(null, "card.e") { it.index == 0 && cardBuffers.check(it.buffer).isFiltered },
            // Ask is the description composer button on this release.
            FilterRule(DescriptionSettings.hideAskSection, "input_composer_button.e"),
            FilterRule(DescriptionSettings.hideAttributesSection, "video_attributes_section"),
            FilterRule(DescriptionSettings.hideCourseProgressSection, "course_progress"),
            FilterRule(DescriptionSettings.hideFeaturedLinksSection, "media_lockup") {
                it.path.startsWith("infocards_section.e")
            },
            FilterRule(
                DescriptionSettings.hideFeaturedVideosSection,
                "structured_description_video_lockup",
            ) {
                it.path.startsWith("infocards_section.e")
            },
            FilterRule(DescriptionSettings.hideHowThisWasMadeSection, "how_this_was_made_section"),
            FilterRule(DescriptionSettings.hideHypePoints, "hype_points_factoid"),
            FilterRule(DescriptionSettings.hideInfoCardsSection, "infocards_section.e"),
            FilterRule(null, "macro_markers_carousel.e") {
                it.index == 0 && macroBuffers.check(it.buffer).isFiltered
            },
            FilterRule(null, "playlist_section.e") {
                it.index == 0 &&
                    (DescriptionSettings.hideExploreSection.value ||
                        playlistBuffers.check(it.buffer).isFiltered)
            },
            FilterRule(DescriptionSettings.hideSubscribeButton, "subscribe_button") {
                it.path.startsWith("infocards_section.e")
            },
            FilterRule(DescriptionSettings.hideTranscriptSection, "transcript_section"),
        )
    }

    override fun isFiltered(component: LithoComponent, group: StringFilterGroup): Boolean {
        val descriptionOpen = "description" in EngagementPanel.openPanelId
        return descriptionOpen &&
            PlayerType.current != PlayerType.WATCH_WHILE_MINIMIZED &&
            super.isFiltered(component, group)
    }
}
