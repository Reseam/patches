// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.x.premium

import app.reseam.patch.Type
import app.reseam.patch.klass
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.settings.returnTrueWhen
import app.reseam.patch.settings.section
import app.reseam.patches.x.core.X
import app.reseam.patches.x.core.XSettings
import app.reseam.patches.x.core.xSettings

val unlockPremium = patch("Unlock Premium") {
    description("Passes the client-side Premium tier checks X runs before video downloads and other Premium-only options.")
    compatibleWith(X)
    settings(xSettings, section("Premium", XSettings.unlockPremium))

    execute {
        premiumTierChecks.forEach { returnTrueWhen(XSettings.unlockPremium) }
    }
}

// The one class that answers tier questions from entitlement names.
val subscriptionFeatures = klass("subscriptionFeatures") {
    strings("feature/twitter_blue_verified", "feature/premium_plus", "feature/premium_basic", "subscriptions_feature_offline_video")
}

// Any tier, and Blue/Plus only.
val premiumTierChecks = subscriptionFeatures.methods("premiumTierChecks") {
    strings("feature/twitter_blue_verified", "feature/premium_plus")
    returns(Type.Boolean)
    params()
}
