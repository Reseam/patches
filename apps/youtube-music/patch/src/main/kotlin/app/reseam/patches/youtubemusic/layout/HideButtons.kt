// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtubemusic.layout

import app.reseam.patch.ExtClass
import app.reseam.patch.Type
import app.reseam.patch.before
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patch.settings.gate
import app.reseam.patches.youtubecommon.hideById
import app.reseam.patches.youtubemusic.core.YOUTUBE_MUSIC
import app.reseam.patches.youtubemusic.core.YouTubeMusicSettings
import app.reseam.patches.youtubemusic.core.YouTubeMusicSettingsPages
import app.reseam.patches.youtubemusic.core.youTubeMusicSettings

val hideButtons = patch("Hide buttons") {
    description("Adds options to hide the cast, search, history and activity feed buttons.")
    compatibleWith(YOUTUBE_MUSIC)
    settings(
        youTubeMusicSettings,
        with(YouTubeMusicSettings) {
            section(YouTubeMusicSettingsPages.Interface, "Buttons",
                hideCastButton, hideSearchButton, hideHistoryButton, hideActivityFeedButton)
        },
    )

    execute {
        // Every cast button is a MediaRouteButton, wherever the app places it.
        gate(YouTubeMusicSettings.hideCastButton) {
            mediaRouteButton.method("setVisibility") { params(Type.Int) }.before { param(0).assign(int(VIEW_GONE)) }
            mediaRouteButton.method("onAttachedToWindow").after {
                thisObject.callVirtual("android.view.View", "setVisibility", "(I)V", int(VIEW_GONE))
            }
        }
        hideById("action_search", YouTubeMusicSettings.hideSearchButton)
        hideById("history_menu_item", YouTubeMusicSettings.hideHistoryButton)
        // The lookups above hide each item once; pages show them again through setVisible.
        bytecode.redirectCalls("android.view.MenuItem", "setVisible", ToolbarButtons.setVisible)

        // Server-sent toolbar buttons, the activity feed among them, are added one at a time by
        // the method that builds their top_bar_menu_item view.
        val itemLayout = resources.id("layout", "top_bar_menu_item")?.toLong() ?: error("layout/top_bar_menu_item is missing")
        val itemView = method("topBarItemView") { literals(itemLayout); returns(Type.View) }
        val addServerButton = method("addServerButton") {
            hasParam("android.view.Menu")
            calls(klass(itemView.owner).method("<init>"))
            calls { owner("android.view.Menu"); name("add") }
        }
        gate(YouTubeMusicSettings.hideActivityFeedButton) {
            addServerButton.alwaysReturn()
        }
    }
}

// androidx MediaRouteButton, which names itself in its log tag.
private val mediaRouteButton = klass("mediaRouteButton") {
    strings("MediaRouteButton")
    extends("android.view.View")
}

private object ToolbarButtons : ExtClass("app.reseam.youtubemusic.layout.ToolbarButtons") {
    val setVisible by static("android.view.MenuItem", Type.Boolean, returns = "android.view.MenuItem")
}
