// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.ads;

import app.reseam.youtube.litho.Filter;
import app.reseam.youtube.litho.FilterGroup.ByteArrayFilterGroup;
import app.reseam.youtube.litho.FilterGroup.StringFilterGroup;
import app.reseam.youtube.litho.StringTrieSearch;

public final class AdsFilter extends Filter {
    private final StringTrieSearch exceptions = new StringTrieSearch();
    private final StringFilterGroup promotionBanner;
    private final ByteArrayFilterGroup promotionBannerBuffer;
    private final StringFilterGroup buyMovieAd;
    private final ByteArrayFilterGroup buyMovieAdBuffer;

    public AdsFilter() {
        exceptions.addPattern("home_video_with_context");
        exceptions.addPattern("related_video_with_context");
        exceptions.addPattern("comment_thread");
        exceptions.addPattern("|comment.");
        exceptions.addPattern("library_recent_shelf");

        final StringFilterGroup carouselAd = new StringFilterGroup(
                "hide_general_ads", true, "carousel_ad"
        );

        final StringFilterGroup generalAds = new StringFilterGroup(
                "hide_general_ads", true,
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
                "shopping_timely_shelf."
        );

        final StringFilterGroup merchandise = new StringFilterGroup(
                "hide_merchandise_banners", true,
                "product_carousel", "shopping_carousel.e"
        );

        final StringFilterGroup movieAds = new StringFilterGroup(
                "hide_general_ads", true,
                "browsy_bar",
                "compact_movie",
                "compact_tvfilm_item",
                "horizontal_movie_shelf",
                "movie_and_show_upsell_card",
                "offer_module_root"
        );

        buyMovieAd = new StringFilterGroup(
                "hide_general_ads", true, "video_lockup_with_attachment.e"
        );
        buyMovieAdBuffer = new ByteArrayFilterGroup(null, false, "FEstorefront");

        final StringFilterGroup viewProducts = new StringFilterGroup(
                "hide_view_products_banner", true,
                "product_item", "products_in_video", "shopping_overlay.e"
        );

        final StringFilterGroup shoppingLinks = new StringFilterGroup(
                "hide_shopping_links", true, "shopping_description_shelf.e"
        );

        promotionBanner = new StringFilterGroup(
                "hide_you_tube_premium_promotions", true, "statement_banner"
        );
        promotionBannerBuffer = new ByteArrayFilterGroup(
                null, false, "img/promos/growth/", "SPunlimited"
        );

        final StringFilterGroup selfSponsor = new StringFilterGroup(
                "hide_self_sponsor_ads", true, "cta_shelf_card"
        );

        addIdentifierCallbacks(carouselAd);
        addPathCallbacks(
                buyMovieAd,
                generalAds,
                merchandise,
                movieAds,
                promotionBanner,
                selfSponsor,
                shoppingLinks,
                viewProducts
        );
    }

    @Override
    public boolean isFiltered(String identifier, String accessibility, String path, byte[] buffer,
                              StringFilterGroup matchedGroup, FilterContentType contentType, int contentIndex) {
        if (matchedGroup == buyMovieAd) {
            return contentIndex == 0 && buffer != null && buyMovieAdBuffer.check(buffer).isFiltered();
        }
        if (matchedGroup == promotionBanner) {
            return contentIndex == 0 && buffer != null && promotionBannerBuffer.check(buffer).isFiltered();
        }
        return !exceptions.matches(path);
    }
}
