// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.sponsorblock.common

import app.reseam.runtime.ChoiceValue
import app.reseam.youtubecommon.settings.SponsorBlockSettings
import app.reseam.youtubecommon.settings.YouTubeCommonStrings

// Undo and membership checks depend on the identity of the fetched API record.
class Segment(
    val uuid: String,
    val category: Category,
    override val start: Long,
    override val end: Long,
) : SegmentRange {
    enum class Category(
        val key: String,
        private val titleText: () -> String,
        private val behavior: ChoiceValue,
    ) {
        SPONSOR(
            "sponsor",
            { YouTubeCommonStrings.sponsorblockCategorySponsor },
            SponsorBlockSettings.sbSponsor,
        ),
        SELFPROMO(
            "selfpromo",
            { YouTubeCommonStrings.sponsorblockCategorySelfpromo },
            SponsorBlockSettings.sbSelfpromo,
        ),
        INTERACTION(
            "interaction",
            { YouTubeCommonStrings.sponsorblockCategoryInteraction },
            SponsorBlockSettings.sbInteraction,
        ),
        HIGHLIGHT(
            "poi_highlight",
            { YouTubeCommonStrings.sponsorblockCategoryHighlight },
            SponsorBlockSettings.sbPoiHighlight,
        ),
        INTRO(
            "intro",
            { YouTubeCommonStrings.sponsorblockCategoryIntro },
            SponsorBlockSettings.sbIntro,
        ),
        OUTRO(
            "outro",
            { YouTubeCommonStrings.sponsorblockCategoryOutro },
            SponsorBlockSettings.sbOutro,
        ),
        PREVIEW(
            "preview",
            { YouTubeCommonStrings.sponsorblockCategoryPreview },
            SponsorBlockSettings.sbPreview,
        ),
        HOOK(
            "hook",
            { YouTubeCommonStrings.sponsorblockCategoryHook },
            SponsorBlockSettings.sbHook,
        ),
        FILLER(
            "filler",
            { YouTubeCommonStrings.sponsorblockCategoryFiller },
            SponsorBlockSettings.sbFiller,
        ),
        MUSIC(
            "music_offtopic",
            { YouTubeCommonStrings.sponsorblockCategoryMusic },
            SponsorBlockSettings.sbMusicOfftopic,
        );

        val title
            get() = titleText()

        val mode
            get() = behavior.value
    }

    val isHighlight: Boolean
        get() = category == Category.HIGHLIGHT

    val isIgnored: Boolean
        get() = category.mode == "ignore"

    val isAutomatic: Boolean
        get() = category.mode.startsWith("skip")
}
