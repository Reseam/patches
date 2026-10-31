// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.settings

import app.reseam.patch.settings.SettingsGroup
import app.reseam.patch.settings.text
import app.reseam.patch.settings.toggle

object ShortsSettings : SettingsGroup(YouTubeStrings, YOUTUBE_KEYS) {
    val hideShortsAiButton by toggle(default = false)
    val hideShortsAutoDubbedLabel by toggle(default = false)
    val hideShortsChannel by toggle(default = false)
    val hideShortsChannelBar by toggle(default = false)
    val hideShortsCommentsButton by toggle(default = false)
    val hideShortsDislikeButton by toggle(default = false)
    val hideShortsFullVideoLinkLabel by toggle(default = false)
    val hideShortsEffectButton by toggle(default = true)
    val hideShortsGreenScreenButton by toggle(default = true)
    val hideShortsNewPostsButton by toggle(default = true)
    val hideShortsHashtagButton by toggle(default = true)
    val hideShortsHome by toggle(default = false)
    val hideShortsInfoPanel by toggle(default = true)
    val hideShortsJoinButton by toggle(default = true)
    val hideShortsLikeButton by toggle(default = false)
    val hideShortsLikeFountain by toggle(default = true)
    val hideShortsLivePreview by toggle(default = false)
    val hideShortsLocationLabel by toggle(default = false)
    val hideShortsPausedOverlayButtons by toggle(default = false)
    val hideShortsPreviewComment by toggle(default = true)
    val hideShortsRemixButton by toggle(default = false)
    val hideShortsSaveSoundButton by toggle(default = true)
    val hideShortsSearchSuggestions by toggle(default = true)
    val hideShortsShareButton by toggle(default = false)
    val hideShortsShopButton by toggle(default = true)
    val hideShortsSoundButton by toggle(default = false)
    val hideShortsSoundMetadataLabel by toggle(default = false)
    val hideShortsStickers by toggle(default = true)
    val hideShortsSubscribeButton by toggle(default = true)
    val hideShortsSuperThanksButton by toggle(default = true)
    val hideShortsTaggedProducts by toggle(default = true)
    val hideShortsUpcomingButton by toggle(default = true)
    val hideShortsUseSoundButton by toggle(default = true)
    val hideShortsUseTemplateButton by toggle(default = true)
    val hideShortsVideoDescription by toggle(default = false)
    val hideShortsVideoTitle by toggle(default = false)
    val shortsAutoplay by toggle(default = false)
    val shortsAutoplayBackground by toggle(default = true)
    val dimShortsOverlayOpacity by text(default = "90")
    val dimShortsOverlayImmersiveMode by toggle(default = true)
    val disableResumingShortsOnStartup by toggle(default = false)
    val playShortsAsVideos by toggle(default = false)
    val turnOffShorts by
        toggle(
            default = false,
            covers =
                listOf(
                    hideShortsHome,
                    hideShortsVideoDescription,
                    disableResumingShortsOnStartup,
                    playShortsAsVideos,
                    AppearanceSettings.hideShortsButton,
                ),
        )
}
