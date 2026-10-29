// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.pinterest.feed

import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.pinterest.core.PINTEREST
import app.reseam.patches.pinterest.core.pinterestSettings
import app.reseam.patches.pinterest.refs.pinRefs
import app.reseam.pinterest.feed.bindings.PinFeeds
import app.reseam.pinterest.settings.FeedSettings
import app.reseam.pinterest.settings.PinterestStrings

val hideShopping = patch("Hide shopping pins") {
    description("Hides shoppable pins.")
    compatibleWith(PINTEREST)
    dependsOn(pinRefs)
    settings(pinterestSettings, section(PinterestStrings.sectionFeed, FeedSettings.hideShopping))

    execute {
        val productGroup = productGroupParser.returnType
        PinFeeds.isProductGroup.implement {
            whenInstanceOf(param(0), productGroup) { returnTrue() }
            returnFalse()
        }
        gate(FeedSettings.hideShopping) {
            filterFeed(PinFeeds.removeShopping)
        }
    }
}

private val productGroupParser = method("dynamicFeedParser") { strings("productgroup", "home_feed_tabs", "bookmark") }
    .point { string("productgroup"); then(within = 6) { invokeStatic { paramCount(1) } } }
    .callee("productGroupParser")
