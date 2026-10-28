// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.settings

import app.reseam.patch.settings.Choice
import app.reseam.patch.settings.SettingsGroup
import app.reseam.patch.settings.Verbatim
import app.reseam.patch.settings.choice
import app.reseam.patch.settings.text
import app.reseam.patch.settings.toggle

object AppearanceSettings : SettingsGroup(YouTubeStrings, YOUTUBE_KEYS) {
    val sbSponsorColor by text(default = "#CC00D400")
    val sbSelfpromoColor by text(default = "#CCFFFF00")
    val sbInteractionColor by text(default = "#CCCC00FF")
    val sbPoiHighlightColor by text(default = "#CCFF1684")
    val sbIntroColor by text(default = "#CC00FFFF")
    val sbOutroColor by text(default = "#CC0202ED")
    val sbPreviewColor by text(default = "#CC008FD6")
    val sbHookColor by text(default = "#CC395699")
    val sbFillerColor by text(default = "#CC7300FF")
    val sbMusicOfftopicColor by text(default = "#CCFF9900")
    val altThumbnailHome by choice(default = "ORIGINAL", choices = thumbnailChoices)
    val altThumbnailSubscriptions by choice(default = "ORIGINAL", choices = thumbnailChoices)
    val altThumbnailLibrary by choice(default = "ORIGINAL", choices = thumbnailChoices)
    val altThumbnailPlayer by choice(default = "ORIGINAL", choices = thumbnailChoices)
    val altThumbnailSearch by choice(default = "ORIGINAL", choices = thumbnailChoices)
    val altThumbnailDearrowApiUrl by
        text(default = "https://dearrow-thumb.ajay.app/api/v1/getThumbnail")
    val altThumbnailDearrowConnectionToast by toggle(default = true)
    val altThumbnailStillsFast by toggle(default = false)
    val altThumbnailStillsTime by
        choice(
            default = "MIDDLE",
            choices =
                listOf(
                    Choice("BEGINNING", YouTubeStrings.altThumbnailStillsTimeBeginning),
                    Choice("MIDDLE", YouTubeStrings.altThumbnailStillsTimeMiddle),
                    Choice("END", YouTubeStrings.altThumbnailStillsTimeEnd),
                ),
        )
    val changeStartPage by
        choice(
            default = "",
            choices =
                listOf(
                    Choice("", YouTubeStrings.changeStartPageDefault),
                    Choice("FEsubscriptions", YouTubeStrings.changeStartPageFesubscriptions),
                    Choice("FEchannels", YouTubeStrings.changeStartPageFechannels),
                    Choice("FEexplore", YouTubeStrings.changeStartPageFeexplore),
                    Choice("FEhistory", YouTubeStrings.changeStartPageFehistory),
                    Choice("FElibrary", YouTubeStrings.changeStartPageFelibrary),
                    Choice(
                        "FEplaylist_aggregation",
                        YouTubeStrings.changeStartPageFeplaylistAggregation,
                    ),
                    Choice("FEactivity", YouTubeStrings.changeStartPageFeactivity),
                    Choice("FEtrending", YouTubeStrings.changeStartPageFetrending),
                    Choice("VLWL", YouTubeStrings.changeStartPageVlwl),
                    Choice("VLLL", YouTubeStrings.changeStartPageVlll),
                    Choice(
                        "com.google.android.youtube.action.open.search",
                        YouTubeStrings.changeStartPageComGoogleAndroidYoutubeActionOpenSearch,
                    ),
                    Choice(
                        "com.google.android.youtube.action.open.shorts",
                        YouTubeStrings.changeStartPageComGoogleAndroidYoutubeActionOpenShorts,
                    ),
                ),
        )
    val changeStartPageAlways by toggle(default = false)
    val changeFormFactor by
        choice(
            default = "",
            choices =
                listOf(
                    Choice("", YouTubeStrings.changeFormFactorDefault),
                    Choice("0", YouTubeStrings.changeFormFactor0),
                    Choice("1", YouTubeStrings.changeFormFactor1),
                    Choice("2", YouTubeStrings.changeFormFactor2),
                    Choice("4", YouTubeStrings.changeFormFactor4),
                ),
        )
    val bypassImageRegionRestrictions by toggle(default = false)
    val hideHomeButton by toggle(default = false)
    val hideShortsButton by toggle(default = true)
    val hideCreateButton by toggle(default = true)
    val hideSubscriptionsButton by toggle(default = false)
    val hideNotificationsButton by toggle(default = false)
    val hideLibraryButton by toggle(default = false)
    val switchCreateWithNotificationsButton by toggle(default = true)
    val hideNavigationButtonLabels by toggle(default = false)
    val narrowNavigationButtons by toggle(default = false)
    val navigationBarAnimations by toggle(default = false)
    val keepNavigationBarVisible by toggle(default = false)
    val disableTranslucentStatusBar by toggle(default = false)
    val disableTranslucentNavigationBarLight by toggle(default = false)
    val disableTranslucentNavigationBarDark by toggle(default = false)
    val hideToolbarCreateButton by toggle(default = true)
    val hideToolbarNotificationButton by toggle(default = false)
    val hideToolbarSearchButton by toggle(default = false)
    val hideToolbarMessagesButton by toggle(default = true)
    val wideSearchBar by toggle(default = false)
    val gradientLoadingScreen by toggle(default = false)
    val splashScreenAnimationStyle by
        choice(
            default = "1",
            choices =
                listOf(
                    Choice("0", YouTubeStrings.splashScreenAnimationStyle0),
                    Choice("1", YouTubeStrings.splashScreenAnimationStyle1),
                    Choice("2", YouTubeStrings.splashScreenAnimationStyle2),
                    Choice("3", YouTubeStrings.splashScreenAnimationStyle3),
                    Choice("4", YouTubeStrings.splashScreenAnimationStyle4),
                    Choice("5", YouTubeStrings.splashScreenAnimationStyle5),
                    Choice("6", YouTubeStrings.splashScreenAnimationStyle6),
                    Choice("7", YouTubeStrings.splashScreenAnimationStyle7),
                    Choice("8", YouTubeStrings.splashScreenAnimationStyle8),
                ),
        )
    val customBrandingName by
        choice(
            default = "2",
            choices =
                listOf(
                    Choice("1", Verbatim("YouTube")),
                    Choice("2", Verbatim("YouTube Reseam")),
                    Choice("3", Verbatim("YT Reseam")),
                    Choice("4", Verbatim("YT")),
                ),
        )
    val customBrandingIcon by
        choice(
            default = "original",
            choices =
                listOf(
                    Choice("original", YouTubeStrings.customBrandingIconOriginal),
                    Choice("reseam", YouTubeStrings.customBrandingIconReseam),
                ),
        )
    val changeHeaderLogo by
        choice(
            default = "default",
            choices =
                listOf(
                    Choice("default", YouTubeStrings.changeHeaderLogoDefault),
                    Choice("premium", YouTubeStrings.changeHeaderLogoPremium),
                    Choice("reseam", YouTubeStrings.changeHeaderLogoReseam),
                ),
        )
    val hideSettingsAccountSwitcher by toggle(default = false)
    val hideSettingsFamilyCenter by toggle(default = false)
    val hideSettingsPremium by toggle(default = false)
    val hideSettingsPurchases by toggle(default = false)
    val hideSettingsBilling by toggle(default = false)
    val hideSettingsBadges by toggle(default = false)
    val hideSettingsYourData by toggle(default = false)
    val hideSettingsConnectedApps by toggle(default = false)
    val hideSettingsExperimentalFeatures by toggle(default = false)
    val hideSettingsWatchOnTv by toggle(default = false)
    val hideSettingsHelp by toggle(default = false)
    val hideSettingsSendFeedback by toggle(default = false)
}
