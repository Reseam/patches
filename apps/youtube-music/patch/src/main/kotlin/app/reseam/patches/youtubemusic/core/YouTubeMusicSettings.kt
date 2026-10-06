// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtubemusic.core

import app.reseam.patch.settings.Choice
import app.reseam.patch.settings.choice
import app.reseam.patch.settings.toggle

// Keys derive from this object's name: `you_tube_music_settings.<property>`. Renaming a property
// resets it for everyone.
object YouTubeMusicSettings {
    val backgroundPlayback by toggle(
        "Background playback",
        summary = "Keeps playing when the app is in the background or the screen is off.",
        default = true,
    )

    val audioOnlyPlayback by toggle(
        "Audio-only playback",
        summary = "Unlocks the \"Don't play music videos\" setting in Data saving.",
        default = true,
    )

    val hideVideoAds by toggle(
        "Hide video ads",
        summary = "Skips advertisements played before and between songs.",
        default = true,
    )

    val hideHomeTab by toggle("Hide Home tab", default = false)
    val hideSamplesTab by toggle("Hide Samples tab", default = false)
    val hideExploreTab by toggle("Hide Explore tab", default = false)
    val hideSearchTab by toggle("Hide Search tab", default = false)
    val hideLibraryTab by toggle("Hide Library tab", default = false)
    val hideUpgradeTab by toggle("Hide Upgrade tab", default = true)

    val startPage by choice(
        "Start page",
        default = "",
        choices = listOf(
            Choice("", "Home"),
            Choice("FEmusic_immersive", "Samples"),
            Choice("FEmusic_explore", "Explore"),
            Choice("FEmusic_library_landing", "Library"),
            Choice("FEmusic_history", "History"),
            Choice("VLLM", "Liked music"),
            Choice("FEmusic_offline", "Downloads"),
        ),
    )

    val hidePremiumPromotions by toggle("Hide Get Music Premium", summary = "Removes it from the account menu and settings.", default = true)

    val hideCategoryBar by toggle("Hide category bar", summary = "Removes the row of mood and genre chips above feeds.", default = false)

    val hideLikeDislikeButton by toggle("Hide like and dislike", default = false)
    val hideDownloadButton by toggle("Hide download", default = false)
    val hideCommentsButton by toggle("Hide comments", default = false)
    val hideSaveButton by toggle("Hide save", default = false)
    val hideShareButton by toggle("Hide share", default = false)
    val hideLyricsButton by toggle("Hide lyrics", default = false)
    val hideMixButton by toggle("Hide mix", default = false)

    val forcePortrait by toggle("Force portrait", summary = "Keeps the app in portrait when the device is rotated.", default = false)

    val hideCastButton by toggle("Hide cast button", default = false)
    val hideSearchButton by toggle("Hide search button", default = false)
    val hideHistoryButton by toggle("Hide history button", default = false)
    val hideActivityFeedButton by toggle("Hide activity feed button", default = false)

    val miniplayerPreviousButton by toggle("Previous button", default = true)
    val miniplayerNextButton by toggle("Next button", default = true)

    val hideMenuPlayNext by toggle("Hide Play next", default = false)
    val hideMenuSaveToPlaylist by toggle("Hide Save to playlist", default = false)
    val hideMenuShare by toggle("Hide Share", default = false)
    val hideMenuStartMix by toggle("Hide Start mix", default = false)
    val hideMenuAddToQueue by toggle("Hide Add to queue", default = false)
    val hideMenuDownload by toggle("Hide Download", default = false)
    val hideMenuLibrary by toggle("Hide Save to library and Remove from library", default = false)
    val hideMenuGoToAlbum by toggle("Hide Go to album", default = false)
    val hideMenuGoToArtist by toggle("Hide Go to artist", default = false)
    val hideMenuSongCredits by toggle("Hide View song credits", default = false)
    val hideMenuSpeedDial by toggle("Hide Pin to Speed dial", default = false)
    val hideMenuNotInterested by toggle("Hide Not interested", default = false)
    val hideMenuDontRecommendArtist by toggle("Hide Don't recommend artist", default = false)
    val hideMenuDismissQueue by toggle("Hide Dismiss queue", default = false)
    val hideMenuReport by toggle("Hide Report", default = false)
    val hideMenuQuality by toggle("Hide Quality", default = false)
    val hideMenuCaptions by toggle("Hide Captions", default = false)
    val hideMenuSleepTimer by toggle("Hide Sleep timer", default = false)

    val hideSpeedDial by toggle("Hide Speed dial", default = false)
    val hideSongVideoSwitch by toggle("Hide song and video switch", default = false)
    val hideShuffleButton by toggle("Hide shuffle button", default = false)
    val hideRepeatButton by toggle("Hide repeat button", default = false)

    val hideSettingsFamilyCenter by toggle("Hide Family Center", default = false)
    val hideSettingsDataSaving by toggle("Hide Data saving", default = false)
    val hideSettingsDownloads by toggle("Hide Downloads & storage", default = false)
    val hideSettingsNotifications by toggle("Hide Notifications", default = false)
    val hideSettingsPrivacy by toggle("Hide Privacy & data", default = false)
    val hideSettingsRecommendations by toggle("Hide Recommendations", default = false)
    val hideSettingsAbout by toggle("Hide About YouTube Music", default = false)

    val customBrandingName by choice(
        "App name",
        summary = "Chooses the name shown by the selected launcher icon.",
        default = "2",
        choices = listOf(Choice("1", "YouTube Music"), Choice("2", "YT Music Reseam"), Choice("3", "YT Music")),
    )
    val customBrandingIcon by choice(
        "App icon",
        summary = "Chooses the launcher icon used by the patched app.",
        default = "original",
        choices = listOf(Choice("original", "Original"), Choice("reseam", "Reseam")),
    )

    val startMinimized by toggle("Start in miniplayer", summary = "Keeps the player minimized when a new song starts.", default = true)

    val playbackSpeed by choice(
        "Playback speed",
        default = "",
        choices = listOf(
            Choice("", "App default"),
            Choice("0.5", "0.5×"), Choice("0.75", "0.75×"), Choice("1.0", "1×"), Choice("1.25", "1.25×"),
            Choice("1.5", "1.5×"), Choice("1.75", "1.75×"), Choice("2.0", "2×"),
        ),
    )

}
