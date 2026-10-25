// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.interaction

import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.before
import app.reseam.patch.classTarget
import app.reseam.patch.dex.Opcode
import app.reseam.patch.dex.methodRef
import app.reseam.patch.dex.opcode
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.points
import app.reseam.patch.reserveLocal
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.patches.youtube.internal.mainActivityHook
import app.reseam.patches.youtube.internal.menuSheetTap
import app.reseam.patches.youtube.internal.routeEndpoint
import app.reseam.patches.youtubecommon.parseProtobuf
import app.reseam.patches.youtubecommon.protobufBytes
import app.reseam.patches.youtubesavetodevice.saveNewDownloads
import app.reseam.youtube.downloads.bindings.SaveToDeviceMenu
import app.reseam.youtubesavetodevice.settings.SaveToDeviceSettings
import app.reseam.youtubesavetodevice.settings.SaveToDeviceStrings

private const val LIST_POPUP_WINDOW = "android.widget.ListPopupWindow"

val saveToDevice = patch("Save to device") {
    description("Adds Save to device to downloaded videos, which saves them as video files in a folder you choose.")
    compatibleWith(YOUTUBE)
    dependsOn(youTubeSettings, mainActivityHook)
    settings(
        youTubeSettings,
        section(
            YouTubeSettingsPages.Downloads,
            SaveToDeviceStrings.sectionSaveToDevice,
            SaveToDeviceSettings.saveFolder,
            SaveToDeviceSettings.saveNewDownloads,
        ),
    )

    execute {
        saveNewDownloads()

        // Every menu for a downloaded video adds Delete from downloads, built from the menu's item for the video;
        // Save to device follows it, built from the Delete item.
        val removeOffline = resources.id("string", "remove_offline")?.toLong() ?: error("string/remove_offline missing")
        val deleteAdds = methods("downloaded video menus") { literals(removeOffline) }.points("delete item added") {
            literal(removeOffline)
            then { invokeStatic { hasParam(Type.Context) } }
            then(within = 2) { invoke { params(Type.Object) } }
        }.all.also { check(it.isNotEmpty()) { "No menu adds Delete from downloads" } }
        val deleteItem = deleteAdds.first().previous { invokeStatic { hasParam(Type.Context) } }
        val menuItem = classTarget("menu item") {
            bytecode.findClass(deleteItem.callee().returnType) ?: error("The menu item class is missing")
        }
        SaveToDeviceMenu.itemBytes.implement { returnValue(protobufBytes(param(0).cast(menuItem.descriptor))) }
        SaveToDeviceMenu.parseItem.implement { returnValue(parseProtobuf(menuItem, "menu item", param(0))) }

        deleteAdds.forEach { add ->
            val ref = checkNotNull(add.instruction.methodRef)
            val toList = add.instruction.opcode in listOf(Opcode.INVOKE_INTERFACE, Opcode.INVOKE_INTERFACE_RANGE)
            add.captureArgumentAs("menu", 0).captureArgumentAs("delete", 1).after {
                val save = SaveToDeviceMenu.saveItem(capture("delete"))
                whenNotNull(save) {
                    if (toList) capture("menu").callInterface(ref.definingClass, ref.name, ref.proto, save)
                    else capture("menu").callVirtual(ref.definingClass, ref.name, ref.proto, save)
                }
            }
        }

        // Menus close before a tapped item's command runs; Save to device runs after, in the menu's activity.
        val rowItem = menuSheetTap.point { opcode(Opcode.IGET_OBJECT); field { type(menuItem.descriptor) } }.field("menu row item")
        menuSheetTap.after {
            whenInstanceOf(param(0), rowItem.owner) { SaveToDeviceMenu.onClick(param(0).cast(rowItem.owner).field(rowItem)) }
        }
        // Popup rows take the command from the item with one helper, whether they keep the item typed or not.
        menuItemClicks.all.also { check(it.isNotEmpty()) { "No popup row routes its item" } }.forEach { click ->
            val item = click.reserveLocal("saveToDeviceItem", Type.Object)
            click.point { invokeStatic { params(menuItem.descriptor) } }.captureArgumentAs("item", 0).before {
                local(item).assign(capture("item"))
            }
            click.after { SaveToDeviceMenu.onClick(local(item)) }
        }
    }
}

private val menuPopup = klass("menu popup") {
    strings("anchor_view")
    hasInstanceField(LIST_POPUP_WINDOW)
}

private val popupDismiss = method("menu popup dismiss") {
    inClass(menuPopup)
    params()
    returns(Type.Void)
    calls { owner(LIST_POPUP_WINDOW); name("dismiss") }
}

// The popup rows' tap handlers, the callers of the popup's dismiss that also route a command.
private val menuItemClicks = methods("menu item clicks") {
    calls(popupDismiss)
    calls(routeEndpoint)
    paramCount(1)
    returns(Type.Void)
}
