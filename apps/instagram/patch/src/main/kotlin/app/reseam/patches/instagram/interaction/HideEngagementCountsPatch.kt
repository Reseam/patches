// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.instagram.interaction

import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.instagram.core.AppearanceSettings
import app.reseam.patches.instagram.core.INSTAGRAM
import app.reseam.patches.instagram.core.instagramSettings
import app.reseam.patches.instagram.core.signatureCheck

private val COUNT_LABELS = listOf(
    ", shouldShowLikeCountInUfi=",
    ", shouldShowCommentCountInUfi=",
    ", shouldShowShareCountInUfi=",
    ", shouldShowRepostCount=",
)

val hideEngagementCounts = patch("Hide engagement counts") {
    description("Hides like, comment, share and repost counts beside the feed action buttons.")
    compatibleWith(INSTAGRAM)
    dependsOn(signatureCheck)
    settings(instagramSettings, section("Appearance", AppearanceSettings.hideEngagementCounts))

    execute {
        val toString = feedUfiConfig.method("toString")
        val countFlags = COUNT_LABELS.map { labelledBoolean(toString, it) }
        gate(AppearanceSettings.hideEngagementCounts) {
            feedUfiConfig.method("<init>").after {
                countFlags.forEach { thisObject.set(it, bool(false)) }
            }
        }
    }
}
