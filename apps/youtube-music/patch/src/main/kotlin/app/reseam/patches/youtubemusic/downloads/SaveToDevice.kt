// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtubemusic.downloads

import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.before
import app.reseam.patch.classTarget
import app.reseam.patch.dex.Opcode
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.reserveLocal
import app.reseam.patch.settings.section
import app.reseam.patches.youtubecommon.parseProtobuf
import app.reseam.patches.youtubecommon.protobufBytes
import app.reseam.patches.youtubemusic.core.YOUTUBE_MUSIC
import app.reseam.patches.youtubemusic.core.YouTubeMusicSettingsPages
import app.reseam.patches.youtubemusic.core.youTubeMusicSettings
import app.reseam.patches.youtubemusic.layout.bindMenuRow
import app.reseam.patches.youtubesavetodevice.saveNewDownloads
import app.reseam.youtubemusic.downloads.bindings.SaveToDeviceMenu
import app.reseam.youtubesavetodevice.settings.SaveToDeviceSettings
import app.reseam.youtubemusic.settings.YouTubeMusicStrings

val saveToDevice = patch("Save to device") {
    description("Adds Save to device to downloaded songs and playlists, which saves them as audio files in a folder you choose.")
    compatibleWith(YOUTUBE_MUSIC)
    settings(
        youTubeMusicSettings,
        section(
            YouTubeMusicSettingsPages.Playback,
            YouTubeMusicStrings.sectionDownloads,
            SaveToDeviceSettings.saveFolder,
            SaveToDeviceSettings.saveNewDownloads,
        ),
    )

    execute {
        saveNewDownloads()
        SaveToDeviceMenu.itemBytes.implement { returnValue(protobufBytes(param(0).cast(menuItem.descriptor))) }
        SaveToDeviceMenu.parseItem.implement { returnValue(parseProtobuf(menuItem, "menu item", param(0))) }
        menuItems.after { SaveToDeviceMenu.addItems(thisObject.fieldOfType(Type.Context), result) }

        // The row reuses its view parameter's register for the item, so the view is kept from entry.
        val view = menuRowClick.reserveLocal("saveToDeviceView", Type.View)
        menuRowClick.before { local(view).assign(param(0)) }
        // A row closes the menu before it runs the item's command; Save to device runs in that place.
        menuRowClick.point { invokeInterface { params(); returns(Type.Void) } }.after {
            SaveToDeviceMenu.onClick(thisObject.field(menuRowItem), local(view))
        }
    }
}

private val menuSheet = klass("menu sheet") { strings("MUSIC_MENU_BOTTOM_SHEET_FRAGMENT_TAG") }

// Builds the items a menu sheet shows from the menu renderer, for the object the menu is about.
private val menuItems = method("menu items") {
    inClass(menuSheet)
    returns(Type.List)
    paramCount(2)
    param(1, Type.Object)
}

private val menuRowClick = method("menu row click") {
    inClass(classTarget("menu row") { bytecode.findClass(bindMenuRow.owner) ?: error("The menu row class is missing") })
    name("onClick")
    params(Type.View)
}

private val menuRowItem = menuRowClick.point("menu row item") { opcode(Opcode.IGET_OBJECT) }.field("menu row item field")

private val menuItem = classTarget("menu item") {
    bytecode.findClass(menuRowItem.type) ?: error("The menu item class is missing")
}
