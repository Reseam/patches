// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.instagram.interaction

import app.reseam.instagram.settings.InstagramStrings
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.instagram.settings.AppearanceSettings
import app.reseam.patches.instagram.InstagramPatchStrings
import app.reseam.patches.instagram.core.INSTAGRAM
import app.reseam.patches.instagram.core.instagramSettings
import app.reseam.patches.instagram.core.signatureCheck

private val COUNT_LABELS = listOf(
    ", shouldShowLikeCountInUfi=",
    ", shouldShowCommentCountInUfi=",
    ", shouldShowShareCountInUfi=",
    ", shouldShowRepostCount=",
)

val hideEngagementCounts = patch(InstagramPatchStrings.hideEngagementCounts) {
    description(InstagramPatchStrings.hideEngagementCountsDescription)
    compatibleWith(INSTAGRAM)
    dependsOn(signatureCheck)
    settings(instagramSettings, section(InstagramStrings.sectionAppearance, AppearanceSettings.hideEngagementCounts))

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
