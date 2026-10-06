// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtubemusic.ads

import app.reseam.patch.ExtClass
import app.reseam.patch.Type
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.youtubemusic.core.YOUTUBE_MUSIC
import app.reseam.patches.youtubemusic.core.YouTubeMusicSettings
import app.reseam.patches.youtubemusic.core.YouTubeMusicSettingsPages
import app.reseam.patches.youtubemusic.core.youTubeMusicSettings
import app.reseam.patches.youtubemusic.layout.VIEW_GONE
import app.reseam.patches.youtubecommon.lithoFilter
import app.reseam.patches.youtubecommon.registerLithoFilter

val premiumPromotions = patch("Premium promotions") {
    description("Hides Get Music Premium in the account menu and in settings.")
    compatibleWith(YOUTUBE_MUSIC)
    settings(youTubeMusicSettings, section(YouTubeMusicSettingsPages.Ads, "Premium", YouTubeMusicSettings.hidePremiumPromotions))

    dependsOn(lithoFilter)

    execute {
        registerLithoFilter(PremiumPromotionsFilter)
        val avatarMenu = resources.id("layout", "avatar_menu")?.toLong() ?: error("layout/avatar_menu is missing")
        val upgradePanel = resources.id("id", "unlimited_panel")?.toInt() ?: error("id/unlimited_panel is missing")
        gate(YouTubeMusicSettings.hidePremiumPromotions) {
            membershipLabel.alwaysReturnNull()
            // The account menu's upgrade footer; the app fills its texts but never touches the panel.
            method("accountMenuView") { literals(avatarMenu); returns(Type.View) }.after {
                capture("result")
                    .callVirtual("android.view.View", "findViewById", "(I)Landroid/view/View;", int(upgradePanel))
                    .callVirtual("android.view.View", "setVisibility", "(I)V", int(VIEW_GONE))
            }
        }
    }
}

// The account menu and the settings screen each show their Get Music Premium row only when this
// label is not empty.
private val membershipLabel = klass("com.google.android.apps.youtube.music.settings.fragment.SettingsHeadersFragment")
    .method("onCreatePreferences")
    .point("membershipRow") { string("settings_header_paid_memberships") }
    .next { invokeVirtual { params(); returns(Type.CharSequence) } }
    .callee("membershipLabel")

private object PremiumPromotionsFilter : ExtClass("app.reseam.youtubemusic.layout.PremiumPromotionsFilter")
