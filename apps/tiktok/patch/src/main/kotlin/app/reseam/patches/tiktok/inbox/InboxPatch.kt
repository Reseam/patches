// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.tiktok.inbox

import app.reseam.patch.Type
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.ToggleSetting
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.tiktok.core.TIKTOK
import app.reseam.patches.tiktok.core.TikTokSettingsPages
import app.reseam.patches.tiktok.core.tiktokSettings
import app.reseam.tiktok.settings.InboxSettings
import app.reseam.tiktok.settings.TikTokStrings

private const val AWEME = "com.ss.android.ugc.aweme"

// Inbox and New followers add each section through an injector whose enable() decides whether it
// appears.
private val SECTIONS: Map<ToggleSetting, List<String>> =
    mapOf(
        InboxSettings.hideInboxStories to listOf("inbox.skylight.InboxSkylightWidgetV2Injector"),
        InboxSettings.hideInboxSuggestions to
            listOf(
                "relation.recuser.inbox.RecommendUserWidgetV2Injector",
                "inbox.v2.container.InboxUserCardWidgetContainerInjector",
                "inbox.v2.container.UserCardWidgetContainerInjector",
                "inbox.v2.container.UserCardWidgetVisibleContainerInjector",
                "relation.recuser.inbox.FollowerUserCardWidgetV2Injector",
                "inbox.widget.multi.FollowerUserCardLoadingWidgetV2Injector",
                "inbox.followerv2.FollowerUserCardWidgetContainerInjector",
                "im.chatlist.impl.feature.maf.ui.MafChatListWidgetV2Injector",
            ),
        InboxSettings.hideInboxBanners to
            listOf(
                "notification.banner.InboxBannerWidgetInjector",
                "notification.banner.InboxLegacyTopBannerWidgetInjector",
                "inbox.v2.container.TopBannerWidgetContainerInjector",
                "notification.view.guidepush.BulletBoardGuideWidgetInjector",
            ),
        InboxSettings.hideInboxShop to listOf("inbox.shop.ShopEntranceMigrationWidgetInjector"),
    )

val inbox =
    patch("Inbox") {
        description(
            "Removes chosen sections from TikTok's Inbox, such as stories, suggested accounts and banners."
        )
        compatibleWith(TIKTOK)
        settings(
            tiktokSettings,
            section(
                TikTokSettingsPages.Inbox,
                TikTokStrings.sectionInbox,
                *SECTIONS.keys.toTypedArray(),
            ),
        )

        execute {
            SECTIONS.forEach { (toggle, injectors) ->
                gate(toggle) { injectors.forEach { sectionEnabled(it).alwaysReturn(false) } }
            }
        }
    }

private fun sectionEnabled(injector: String) =
    klass("$AWEME.$injector").method("enable") { returns(Type.Boolean) }
