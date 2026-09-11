// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.x.premium

import app.reseam.patch.Type
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.returnNullWhen
import app.reseam.patch.settings.section
import app.reseam.patches.x.core.X
import app.reseam.patches.x.core.XSettings
import app.reseam.patches.x.core.xSettings
import app.reseam.patches.x.featureswitches.disableFeatureSwitches
import app.reseam.patches.x.featureswitches.featureSwitchOverrides

val hidePremiumUpsells = patch("Hide Premium upsells") {
    description("Removes the Upgrade button and discount pills, the Get Verified prompts, and the Premium upsells in the composer.")
    compatibleWith(X)
    dependsOn(featureSwitchOverrides)
    settings(xSettings, section("Premium", XSettings.hidePremiumUpsells))

    execute {
        disableFeatureSwitches(
            XSettings.hidePremiumUpsells,
            "subscriptions_upsells_get_verified_drawer_card_enabled",
            "subscriptions_upsells_get_verified_drawer_pill_enabled",
            "subscriptions_upsells_home_nav_follow_button_enabled",
            "subscriptions_upsells_premium_home_nav_offer_enabled",
            "subscriptions_upsells_long_video_upload_composer_enabled",
        )
        // Both return a model or null; the UI already handles null as "nothing to show".
        homeNavUpgrade.returnNullWhen(XSettings.hidePremiumUpsells)
        premiumOffer.returnNullWhen(XSettings.hidePremiumUpsells)
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
