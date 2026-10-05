// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.x.premium

import app.reseam.patch.Type
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.x.core.X
import app.reseam.patches.x.core.XSettings
import app.reseam.patches.x.core.xSettings
import app.reseam.patches.x.featureswitches.disableFeatureSwitches
import app.reseam.patches.x.featureswitches.featureSwitchOverrides

val hidePremiumUpsells = patch("Hide Premium upsells") {
    description("Removes the Upgrade button and discount pills, the Get Verified prompts, the Premium upsells on profiles, posts, and in the composer, and the paywall at launch.")
    compatibleWith(X)
    dependsOn(featureSwitchOverrides)
    settings(xSettings, section("Premium", XSettings.hidePremiumUpsells, XSettings.hideAppOpenPaywall))

    execute {
        disableFeatureSwitches(
            XSettings.hidePremiumUpsells,
            "subscriptions_upsells_get_verified_drawer_card_enabled",
            "subscriptions_upsells_get_verified_drawer_pill_enabled",
            "subscriptions_upsells_home_nav_follow_button_enabled",
            "subscriptions_upsells_premium_home_nav_offer_enabled",
            "subscriptions_upsells_long_video_upload_composer_enabled",
            "subscriptions_upsells_premium_home_nav_enabled",
            "subscriptions_upsells_drawer_discount_enabled",
            "subscriptions_upsells_get_verified_drawer_card_discount_enabled",
            "subscriptions_upsells_get_verified_profile",
            "subscriptions_upsells_get_verified_profile_card",
            "subscriptions_upsells_get_verified_profile_rotation_enabled",
            "subscriptions_upsells_get_verified_profile_discount_own_enabled",
            "subscriptions_upsells_get_verified_profile_discount_visitor_enabled",
            "subscriptions_upsells_verified_profile_visitor_upsell_enabled",
            "subscriptions_upsells_verified_profile_visitor_upsell_redesign_enabled",
            "subscriptions_upsell_visitor_verified_profile",
            "subscriptions_upsells_analytics_profile_enabled",
            "subscriptions_upsells_full_hd_video_upload_composer_enabled",
            "subscriptions_upsells_full_hd_video_upload_discount_enabled",
            "subscriptions_upsells_articles_post_composer_promo_variant_enabled",
            // Subscribers with the feature skip these reads; only the upsell path consults them.
            "subscriptions_upsells_highlights_enabled",
            "subscriptions_upsells_bookmark_folders_enabled",
            "x_lite_dismiss_ad_premium_upsell_enabled",
            "x_lite_quick_promote_premium_paywall_enabled",
        )
        disableFeatureSwitches(XSettings.hideAppOpenPaywall, "premium_paywall_on_app_load_journey_enabled")
        // Both return a model or null; the UI already handles null as "nothing to show".
        gate(XSettings.hidePremiumUpsells) {
            homeNavUpgrade.alwaysReturnNull()
            premiumOffer.alwaysReturnNull()
        }
    }
}

// Builds the top-right Upgrade button model, with or without the current discount.
val homeNavUpgrade = method("homeNavUpgrade") {
    strings("subscriptions_upsells_premium_home_nav_offer_enabled")
    returns(Type.Object)
}

// Loads the discount that feeds the "N% off" pills in the drawer and the composer.
val premiumOffer = method("premiumOffer") {
    strings("Failed to load catalog for drawer discount pill: ")
    returns(Type.Object)
}
