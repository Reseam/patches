// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.interaction

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
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
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
    dependsOn(youTubeSettings)
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

        // A downloaded video's menu lists its state's action, then Delete from downloads, built from the
        // menu's item for the video; Save to device follows them, built from the same item.
        val removeOffline = resources.id("string", "remove_offline") ?: error("string/remove_offline missing")
        val offlineVideoMenu = method("offline video menu") {
            literals(removeOffline.toLong())
            params(Type.Object)
            returns(Type.Object)
        }
        val deleteItem = offlineVideoMenu.point { literal(removeOffline.toLong()) }.next { invokeStatic { hasParam(Type.Context) } }
        val menuItem = classTarget("menu item") {
            bytecode.findClass(deleteItem.callee().returnType) ?: error("The menu item class is missing")
        }
        SaveToDeviceMenu.itemBytes.implement { returnValue(protobufBytes(param(0).cast(menuItem.descriptor))) }
        SaveToDeviceMenu.parseItem.implement { returnValue(parseProtobuf(menuItem, "menu item", param(0))) }

        val context = offlineVideoMenu.reserveLocal("saveToDeviceContext", Type.Context)
        val template = offlineVideoMenu.reserveLocal("saveToDeviceTemplate", Type.Object)
        deleteItem.captureArgumentAs("context", 0, Type.Context).captureArgumentAs("template", 1).before {
            local(context).assign(capture("context"))
            local(template).assign(capture("template"))
        }
        val addDelete = deleteItem.next { invokeVirtual { params(Type.Object); returns(Type.Void) } }
        addDelete.captureArgumentAs("menu", 0).after {
            val save = SaveToDeviceMenu.saveItem(local(context), local(template))
            whenNotNull(save) { capture("menu").call(addDelete.callee(), save) }
        }

        // A tap routes the item's command, if any, then closes the popup; Save to device runs after that.
        val item = menuItemClick.reserveLocal("saveToDeviceItem", Type.Object)
        menuItemClick.point { opcode(Opcode.IGET_OBJECT); field { type(menuItem.descriptor) } }
            .captureAs("item")
            .after { local(item).assign(capture("item")) }
        // The view the menu opened from belongs to the shown menu, which closing drops, so it is read before.
        val anchor = menuItemClick.reserveLocal("saveToDeviceAnchor", Type.View)
        val dismiss = menuItemClick.point { calls(popupDismiss) }.captureArgumentAs("popup", 0)
        dismiss.before {
            local(anchor).assign(capture("popup").fieldOfType(shownMenuAnchor.owner).call(shownMenuAnchor))
        }
        dismiss.after { SaveToDeviceMenu.onClick(local(item), local(anchor)) }
    }
}

private val menuPopup = klass("menu popup") {
    strings("anchor_view")
    hasInstanceField(LIST_POPUP_WINDOW)
}

// The shown menu's anchor view, which the popup reads for its logging.
private val shownMenuAnchor = method("menu popup anchor logging") {
    inClass(menuPopup)
    strings("anchor_view")
}
    .point { string("anchor_view") }
    .next { invokeInterface { params(); returns(Type.View) } }
    .callee("shown menu anchor")

private val popupDismiss = method("menu popup dismiss") {
    inClass(menuPopup)
    params()
    returns(Type.Void)
    calls { owner(LIST_POPUP_WINDOW); name("dismiss") }
}

// The popup's tap listener receives the tapped row; the popup's other callers take views or configurations.
private val menuItemClick = method("menu item click") {
    calls(popupDismiss)
    paramCount(1)
    returns(Type.Void)
    custom { !parameterTypes.single().startsWith("Landroid/") }
}
