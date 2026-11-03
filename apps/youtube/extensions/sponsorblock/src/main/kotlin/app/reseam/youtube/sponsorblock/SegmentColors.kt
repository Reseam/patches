// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.sponsorblock

import app.reseam.bundle.color
import app.reseam.youtube.settings.AppearanceSettings
import app.reseam.youtube.sponsorblock.common.Segment

internal val Segment.Category.color: Int
    get() =
        when (this) {
            Segment.Category.SPONSOR -> AppearanceSettings.sbSponsorColor
            Segment.Category.SELFPROMO -> AppearanceSettings.sbSelfpromoColor
            Segment.Category.INTERACTION -> AppearanceSettings.sbInteractionColor
            Segment.Category.HIGHLIGHT -> AppearanceSettings.sbPoiHighlightColor
            Segment.Category.INTRO -> AppearanceSettings.sbIntroColor
            Segment.Category.OUTRO -> AppearanceSettings.sbOutroColor
            Segment.Category.PREVIEW -> AppearanceSettings.sbPreviewColor
            Segment.Category.HOOK -> AppearanceSettings.sbHookColor
            Segment.Category.FILLER -> AppearanceSettings.sbFillerColor
            Segment.Category.MUSIC -> AppearanceSettings.sbMusicOfftopicColor
        }.color
