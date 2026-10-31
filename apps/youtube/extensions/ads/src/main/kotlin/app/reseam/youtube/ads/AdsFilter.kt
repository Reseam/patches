// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.ads

import app.reseam.runtime.Export
import app.reseam.youtube.litho.Filter
import app.reseam.youtube.litho.FilterGroup.ByteArrayFilterGroup
import app.reseam.youtube.litho.FilterGroup.StringFilterGroup
import app.reseam.youtube.litho.LithoComponent
import app.reseam.youtube.litho.StringTrieSearch
import app.reseam.youtube.litho.matches
import app.reseam.youtube.settings.AdsSettings

@Export
class AdsFilter : Filter() {
    private val exceptions =
        StringTrieSearch<Unit>().apply {
            listOf(
                    "home_video_with_context",
                    "related_video_with_context",
                    "comment_thread",
                    "|comment.",
                    "library_recent_shelf",
                )
                .forEach(::addPattern)
        }
    private val promotionBannerBuffer =
        ByteArrayFilterGroup(null, "img/promos/growth/", "SPunlimited")
    private val promotionBanner =
        StringFilterGroup(AdsSettings.hideYouTubePremiumPromotions, "statement_banner")

    init {
        addIdentifierCallbacks(StringFilterGroup(AdsSettings.hideGeneralAds, "carousel_ad"))
        addPathCallbacks(
            StringFilterGroup(
                AdsSettings.hideGeneralAds,
                "_ad_with",
                "_buttoned_layout",
                "ads_video_with_context",
                "banner_text_icon",
                "brand_video_shelf",
                "brand_video_singleton",
                "carousel_footered_layout",
                "carousel_headered_layout",
                "compact_landscape_image_layout",
                "composite_concurrent_carousel_layout",
                "full_width_portrait_image_layout",
                "full_width_square_image_carousel_layout",
                "full_width_square_image_layout",
                "hero_promo_image",
                "image_button_group_layout",
                "landscape_image_carousel_layout",
                "landscape_image_wide_button_layout",
                "primetime_promo",
                "product_details",
                "square_image_layout",
                "text_image_button_layout",
                "text_image_no_button_layout",
                "video_display_button_group_layout",
                "video_display_carousel_button_group_layout",
                "video_display_carousel_buttoned_short_dr_layout",
                "video_display_full_buttoned_short_dr_layout",
                "video_display_full_layout",
                "watch_metadata_app_promo",
                "shopping_timely_shelf.",
            ),
            StringFilterGroup(
                AdsSettings.hideMerchandiseBanners,
                "product_carousel",
                "shopping_carousel.e",
            ),
            promotionBanner,
            StringFilterGroup(AdsSettings.hideSelfSponsorAds, "cta_shelf_card"),
            StringFilterGroup(AdsSettings.hideShoppingLinks, "shopping_description_shelf.e"),
            StringFilterGroup(
                AdsSettings.hideViewProductsBanner,
                "product_item",
                "products_in_video",
                "shopping_overlay.e",
            ),
        )
    }

    override fun isFiltered(component: LithoComponent, group: StringFilterGroup): Boolean =
        when (group) {
            promotionBanner ->
                component.index == 0 && promotionBannerBuffer.check(component.buffer).isFiltered
            else -> !exceptions.matches(component.path)
        }
}
