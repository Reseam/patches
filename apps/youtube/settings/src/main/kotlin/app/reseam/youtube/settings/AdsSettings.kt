// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.settings

import app.reseam.patch.settings.SettingsGroup
import app.reseam.patch.settings.toggle

object AdsSettings : SettingsGroup(YouTubeStrings, YOUTUBE_KEYS) {
    val hideEndScreenStoreBanner by toggle(default = true)
    val hideFullscreenAds by toggle(default = true)
    val hideGeneralAds by toggle(default = true)
    val hideMerchandiseBanners by toggle(default = true)
    val hidePaidPromotionLabel by toggle(default = true)
    val hidePlayerPopupAds by toggle(default = true)
    val hideSelfSponsorAds by toggle(default = true)
    val hideShoppingLinks by toggle(default = true)
    val hideViewProductsBanner by toggle(default = true)
    val hideYouTubePremiumPromotions by toggle(default = true)
    val hideVideoAds by toggle(default = true)
}
