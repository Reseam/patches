// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.settings

import app.reseam.patch.settings.SettingsGroup
import app.reseam.patch.settings.toggle

object DescriptionSettings : SettingsGroup(YouTubeStrings, YOUTUBE_KEYS) {
    val hideAttributesSection by toggle(default = false)
    val hideFeaturedPlacesSection by toggle(default = false)
    val hideGamingSection by toggle(default = false)
    val hideMusicSection by toggle(default = false)
    val hideQuizzesSection by toggle(default = false)
    val hideAiGeneratedVideoSummarySection by toggle(default = false)
    val hideAskSection by toggle(default = false)
    val hideChaptersSection by toggle(default = true)
    val hideCourseProgressSection by toggle(default = false)
    val hideExploreCourseSection by toggle(default = false)
    val hideExplorePodcastSection by toggle(default = false)
    val hideExploreSection by toggle(default = true)
    val hideFeaturedLinksSection by toggle(default = false)
    val hideFeaturedVideosSection by toggle(default = false)
    val hideHowThisWasMadeSection by toggle(default = false)
    val hideHypePoints by toggle(default = false)
    val hideInfoCardsSection by toggle(default = true)
    val hideKeyConceptsSection by toggle(default = false)
    val hideSubscribeButton by toggle(default = false)
    val hideTranscriptSection by toggle(default = true)
}
