// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtubemusic.settings

import app.reseam.patch.settings.Choice
import app.reseam.patch.settings.SettingsGroup
import app.reseam.patch.settings.Verbatim
import app.reseam.patch.settings.choice
import app.reseam.patch.settings.toggle

// Keys derive from this object's name: `you_tube_music_settings.<property>`. Renaming a property
// resets it for everyone.
object YouTubeMusicSettings : SettingsGroup(YouTubeMusicStrings) {
    val backgroundPlayback by toggle(default = true)

    val audioOnlyPlayback by toggle(default = true)

    val unlockDownloads by toggle(default = true)

    val hideVideoAds by toggle(default = true)

    val hideHomeTab by toggle(default = false)
    val hideSamplesTab by toggle(default = false)
    val hideExploreTab by toggle(default = false)
    val hideSearchTab by toggle(default = false)
    val hideLibraryTab by toggle(default = false)
    val hideUpgradeTab by toggle(default = true)

    val startPage by
        choice(
            default = "",
            choices =
                listOf(
                    Choice("", YouTubeMusicStrings.startPageDefault),
                    Choice("FEmusic_immersive", YouTubeMusicStrings.startPageFemusicImmersive),
                    Choice("FEmusic_explore", YouTubeMusicStrings.startPageFemusicExplore),
                    Choice(
                        "FEmusic_library_landing",
                        YouTubeMusicStrings.startPageFemusicLibraryLanding,
                    ),
                    Choice("FEmusic_history", YouTubeMusicStrings.startPageFemusicHistory),
                    Choice("VLLM", YouTubeMusicStrings.startPageVllm),
                    Choice("FEmusic_offline", YouTubeMusicStrings.startPageFemusicOffline),
                ),
        )

    val hidePremiumPromotions by toggle(default = true)

    val hideCategoryBar by toggle(default = false)

    val hideLikeDislikeButton by toggle(default = false)
    val hideDownloadButton by toggle(default = false)
    val hideCommentsButton by toggle(default = false)
    val hideSaveButton by toggle(default = false)
    val hideShareButton by toggle(default = false)
    val hideLyricsButton by toggle(default = false)
    val hideMixButton by toggle(default = false)

    val forcePortrait by toggle(default = false)

    val hideCastButton by toggle(default = false)
    val hideSearchButton by toggle(default = false)
    val hideHistoryButton by toggle(default = false)
    val hideActivityFeedButton by toggle(default = false)

    val miniplayerPreviousButton by toggle(default = true)
    val miniplayerNextButton by toggle(default = true)

    val hideMenuPlayNext by toggle(default = false)
    val hideMenuSaveToPlaylist by toggle(default = false)
    val hideMenuShare by toggle(default = false)
    val hideMenuStartMix by toggle(default = false)
    val hideMenuAddToQueue by toggle(default = false)
    val hideMenuDownload by toggle(default = false)
    val hideMenuLibrary by toggle(default = false)
    val hideMenuGoToAlbum by toggle(default = false)
    val hideMenuGoToArtist by toggle(default = false)
    val hideMenuSongCredits by toggle(default = false)
    val hideMenuSpeedDial by toggle(default = false)
    val hideMenuNotInterested by toggle(default = false)
    val hideMenuDontRecommendArtist by toggle(default = false)
    val hideMenuDismissQueue by toggle(default = false)
    val hideMenuReport by toggle(default = false)
    val hideMenuQuality by toggle(default = false)
    val hideMenuCaptions by toggle(default = false)
    val hideMenuSleepTimer by toggle(default = false)

    val hideSpeedDial by toggle(default = false)
    val hideSongVideoSwitch by toggle(default = false)
    val hideShuffleButton by toggle(default = false)
    val hideRepeatButton by toggle(default = false)

    val hideSettingsFamilyCenter by toggle(default = false)
    val hideSettingsDataSaving by toggle(default = false)
    val hideSettingsDownloads by toggle(default = false)
    val hideSettingsNotifications by toggle(default = false)
    val hideSettingsPrivacy by toggle(default = false)
    val hideSettingsRecommendations by toggle(default = false)
    val hideSettingsAbout by toggle(default = false)

    val customBrandingName by
        choice(
            default = "2",
            choices =
                listOf(
                    Choice("1", Verbatim("YouTube Music")),
                    Choice("2", Verbatim("YT Music Reseam")),
                    Choice("3", Verbatim("YT Music")),
                ),
        )
    val customBrandingIcon by
        choice(
            default = "original",
            choices =
                listOf(
                    Choice("original", YouTubeMusicStrings.customBrandingIconOriginal),
                    Choice("reseam", YouTubeMusicStrings.customBrandingIconReseam),
                ),
        )

    val startMinimized by toggle(default = false)

    val playbackSpeed by
        choice(
            default = "",
            choices =
                listOf(
                    Choice("", YouTubeMusicStrings.playbackSpeedDefault),
                    Choice("0.5", YouTubeMusicStrings.playbackSpeed05),
                    Choice("0.75", YouTubeMusicStrings.playbackSpeed075),
                    Choice("1.0", YouTubeMusicStrings.playbackSpeed10),
                    Choice("1.25", YouTubeMusicStrings.playbackSpeed125),
                    Choice("1.5", YouTubeMusicStrings.playbackSpeed15),
                    Choice("1.75", YouTubeMusicStrings.playbackSpeed175),
                    Choice("2.0", YouTubeMusicStrings.playbackSpeed20),
                ),
        )
}
