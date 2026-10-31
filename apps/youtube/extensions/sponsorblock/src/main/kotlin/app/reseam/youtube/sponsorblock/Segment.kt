// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.sponsorblock

import app.reseam.bundle.color
import app.reseam.runtime.ChoiceValue
import app.reseam.runtime.TextValue
import app.reseam.youtube.settings.AppearanceSettings
import app.reseam.youtube.settings.VideoSettings
import app.reseam.youtube.settings.YouTubeStrings

// Undo and membership checks depend on the identity of the fetched API record.
internal class Segment(
    val uuid: String,
    val category: Category,
    override val start: Long,
    override val end: Long,
) : SegmentRange {
    enum class Category(
        val key: String,
        private val titleText: () -> String,
        private val behavior: ChoiceValue,
        private val colorSetting: TextValue,
    ) {
        SPONSOR(
            "sponsor",
            { YouTubeStrings.sponsorblockCategorySponsor },
            VideoSettings.sbSponsor,
            AppearanceSettings.sbSponsorColor,
        ),
        SELFPROMO(
            "selfpromo",
            { YouTubeStrings.sponsorblockCategorySelfpromo },
            VideoSettings.sbSelfpromo,
            AppearanceSettings.sbSelfpromoColor,
        ),
        INTERACTION(
            "interaction",
            { YouTubeStrings.sponsorblockCategoryInteraction },
            VideoSettings.sbInteraction,
            AppearanceSettings.sbInteractionColor,
        ),
        HIGHLIGHT(
            "poi_highlight",
            { YouTubeStrings.sponsorblockCategoryHighlight },
            VideoSettings.sbPoiHighlight,
            AppearanceSettings.sbPoiHighlightColor,
        ),
        INTRO(
            "intro",
            { YouTubeStrings.sponsorblockCategoryIntro },
            VideoSettings.sbIntro,
            AppearanceSettings.sbIntroColor,
        ),
        OUTRO(
            "outro",
            { YouTubeStrings.sponsorblockCategoryOutro },
            VideoSettings.sbOutro,
            AppearanceSettings.sbOutroColor,
        ),
        PREVIEW(
            "preview",
            { YouTubeStrings.sponsorblockCategoryPreview },
            VideoSettings.sbPreview,
            AppearanceSettings.sbPreviewColor,
        ),
        HOOK(
            "hook",
            { YouTubeStrings.sponsorblockCategoryHook },
            VideoSettings.sbHook,
            AppearanceSettings.sbHookColor,
        ),
        FILLER(
            "filler",
            { YouTubeStrings.sponsorblockCategoryFiller },
            VideoSettings.sbFiller,
            AppearanceSettings.sbFillerColor,
        ),
        MUSIC(
            "music_offtopic",
            { YouTubeStrings.sponsorblockCategoryMusic },
            VideoSettings.sbMusicOfftopic,
            AppearanceSettings.sbMusicOfftopicColor,
        );

        val title
            get() = titleText()

        val mode
            get() = behavior.value

        val color: Int
            get() = colorSetting.color
    }

    val isHighlight: Boolean
        get() = category == Category.HIGHLIGHT

    val isIgnored: Boolean
        get() = category.mode == "ignore"

    val isAutomatic: Boolean
        get() = category.mode.startsWith("skip")
}
