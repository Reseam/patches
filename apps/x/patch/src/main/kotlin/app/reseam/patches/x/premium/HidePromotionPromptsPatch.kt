// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.x.premium

import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.x.core.X
import app.reseam.patches.x.core.XSettings
import app.reseam.patches.x.core.xSettings
import app.reseam.patches.x.featureswitches.disableFeatureSwitches
import app.reseam.patches.x.featureswitches.featureSwitchOverrides

val hidePromotionPrompts = patch("Hide promotion prompts") {
    description("Hides the Boost and Promote prompts on your posts.")
    compatibleWith(X)
    dependsOn(featureSwitchOverrides)
    settings(xSettings, section("Premium", XSettings.hidePromotionPrompts))

    execute {
        disableFeatureSwitches(
            XSettings.hidePromotionPrompts,
            "x_lite_quick_promote_enabled",
            "android_tweet_promote_button_boost_cta_creation_enabled",
            "android_tweet_promote_button_boost_cta_render_enabled",
            "android_tweet_promote_button_booster_status_label_enabled",
            "android_tweet_promote_button_social_context_label_enabled",
        )
    }
}
