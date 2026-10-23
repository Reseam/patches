// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtubemusic.layout

import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.classTarget
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.reserveLocal
import app.reseam.patch.settings.section
import app.reseam.patches.youtubecommon.lithoFilter
import app.reseam.patches.youtubecommon.registerLithoFilter
import app.reseam.patches.youtubemusic.core.YOUTUBE_MUSIC
import app.reseam.patches.youtubemusic.core.YouTubeMusicSettingsPages
import app.reseam.patches.youtubemusic.core.youTubeMusicSettings
import app.reseam.youtubemusic.layout.bindings.MenuItems
import app.reseam.youtubemusic.layout.bindings.MenuTilesFilter
import app.reseam.youtubemusic.settings.YouTubeMusicSettings
import app.reseam.youtubemusic.settings.YouTubeMusicStrings

val hideMenuItems = patch("Hide menu items") {
    description("Adds options to hide items from the long-press and three-dot menus.")
    compatibleWith(YOUTUBE_MUSIC)
    dependsOn(lithoFilter)
    settings(
        youTubeMusicSettings,
        with(YouTubeMusicSettings) {
            section(
                YouTubeMusicSettingsPages.Menus, YouTubeMusicStrings.sectionMenuItems,
                hideMenuPlayNext, hideMenuSaveToPlaylist, hideMenuShare, hideMenuStartMix, hideMenuAddToQueue,
                hideMenuDownload, hideMenuLibrary, hideMenuGoToAlbum, hideMenuGoToArtist, hideMenuSongCredits,
                hideMenuSpeedDial, hideMenuNotInterested, hideMenuDontRecommendArtist, hideMenuDismissQueue,
                hideMenuReport, hideMenuQuality, hideMenuCaptions, hideMenuSleepTimer,
            )
        },
    )

    execute {
        registerLithoFilter(MenuTilesFilter)
        val icon = bindMenuRow.reserveLocal("menuIcon", Type.Object)
        bindMenuRow.point("rowIcon") { invokeStatic { owner(menuIcon.descriptor); params(Type.Int) } }
            .next { resultOf() }.captureAs("icon").after { local(icon).assign(capture("icon")) }
        bindMenuRow.after {
            whenNotNull(local(icon)) {
                MenuItems.bind(thisObject.call(menuRowView), local(icon).cast("java.lang.Enum"))
            }
        }
    }
}

// The icon type the server names for every menu entry; the constant names survive obfuscation.
private val menuIcon = klass("menuIcon") {
    strings("QUEUE_PLAY_NEXT", "OFFLINE_DOWNLOAD", "WATCH_HISTORY_CAIRO")
    extends("java.lang.Enum")
}

// Binds one menu row. Toggle rows read their state from these context keys.
internal val bindMenuRow = method("bindMenuRow") {
    strings("toggleMenuItemMutations", "sharedToggleMenuItemMutations")
    params()
    returns(Type.Void)
}

// The container the menu lists; the private getter returns the row inside it.
private val menuRowView = method("menuRowView") {
    flags(AccessFlags.PUBLIC)
    inClass(classTarget("menuRow") { bytecode.findClass(bindMenuRow.owner) ?: error("The menu row class is missing") })
    params()
    returns(Type.View)
}
