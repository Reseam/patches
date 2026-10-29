// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.pinterest.download

import app.reseam.patch.CodeScope
import app.reseam.patch.FieldTarget
import app.reseam.patch.MethodTarget
import app.reseam.patch.PatchRuntime
import app.reseam.patch.Type
import app.reseam.patch.ValueRef
import app.reseam.patch.after
import app.reseam.patch.dex.DexClass
import app.reseam.patch.dex.Opcode
import app.reseam.patch.dex.methodRef
import app.reseam.patch.enumConstant
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.methodTarget
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patches.pinterest.core.PINTEREST
import app.reseam.patches.pinterest.feed.dynamicFeedParsed
import app.reseam.patches.pinterest.feed.feedModels
import app.reseam.patches.pinterest.refs.boardValue
import app.reseam.patches.pinterest.refs.holdsField
import app.reseam.patches.pinterest.refs.pinValue
import app.reseam.pinterest.download.bindings.BoardDownloads

// The item takes its title and index, seven optional fields, then its icon.
private const val OPTIONAL_ITEM_FIELDS = 7

val downloadBoard = patch("Download board") {
    description("Downloads the loaded pins of a board.")
    compatibleWith(PINTEREST)
    dependsOn(downloadPins)

    execute {
        val title = resources.addString("reseam_download_board", "Download board")?.toInt() ?: error("could not add the item title")
        val board = boardValue("id").parameterTypes.single()
        val icon = klass(optionItem.parameterTypes[OPTIONAL_ITEM_FIELDS + 2]).enumConstant("DOWNLOAD")

        dynamicFeedParsed.after { BoardDownloads.remember(thisObject.field(feedModels)) }
        parsedPins.forEach { after { BoardDownloads.rememberPin(result) } }

        // Board screens build the menu from their board; other callers show menus for no single board.
        val screens = boardMenuCalls.all.filter { holdsField(it.owner, board) }.ifEmpty { error("No board screen builds the board menu") }
        val viewIfBound = screens.first().point { invokeVirtual { name("getViewIfBound") } }.callee("viewIfBound")
        val closeSheet = closeOptionsSheet(screens.first(), viewIfBound)
        BoardDownloads.closeMenu.implement {
            val view = param(0).cast(viewIfBound.owner).call(viewIfBound)
            whenInstanceOf(view, closeSheet.owner) { view.cast(closeSheet.owner).call(closeSheet) }
            returnVoid()
        }

        screens.forEach { screen ->
            screen.point { calls(boardMenu) }.next { resultOf() }.captureAs("menu").after {
                val menu = capture("menu")
                val items = menu.field(optionItems)
                val index = items.callInterface(Type.List, "size", "()I")
                val action = BoardDownloads.menuAction(menu.field(actionHandler), index, thisObject.fieldOfType(board), thisObject)
                menu.assign(optionGroup(menu.field(optionLabel), BoardDownloads.withItem(items, downloadItem(title, index, icon)), action))
            }
        }
    }
}

private fun CodeScope.downloadItem(title: Int, index: ValueRef, icon: FieldTarget) =
    newInstance(optionItem.owner, optionItem.proto, int(title), index, *Array(OPTIONAL_ITEM_FIELDS) { nullObject }, staticField(icon), int(0))

private fun CodeScope.optionGroup(label: ValueRef, items: ValueRef, action: ValueRef) =
    newInstance(optionGroupClass.descriptor, "(${optionLabel.type}Ljava/util/List;Lkotlin/jvm/functions/Function1;)V", label, items, action)

private val parsedPins = methods("parsedPins") {
    strings("is_promoted", "created_at")
    paramCount(1)
    returns(pinValue("id").parameterTypes.single())
}

private val boardMenu = method("boardMenu") {
    accesses { name("lego_board_overflow_options_label") }
    accesses { name("board_overflow_edit_info_and_settings") }
}

private val boardMenuCalls = methods("boardMenuCalls") { calls(boardMenu) }

private val optionItem = boardMenu.point { invokeDirect { name("<init>"); paramCount(OPTIONAL_ITEM_FIELDS + 4) } }.callee("optionItem")

private val optionGroupClass = klass("optionGroup") { strings("OptionGroup(label=", ", optionItems=", ", actionHandler=") }

private fun optionGroupField(label: String) =
    optionGroupClass.method("toString").point { string(label) }.next { opcode(Opcode.IGET_OBJECT) }.field(label.trim('(', ',', ' ', '='))

private val optionLabel = optionGroupField("OptionGroup(label=")
private val optionItems = optionGroupField(", optionItems=")
private val actionHandler = optionGroupField(", actionHandler=")

// The screen shows the sheet through its view, an options sheet host whose no-argument method closes it.
private fun closeOptionsSheet(screen: MethodTarget, viewIfBound: MethodTarget) = methodTarget("closeOptionsSheet") {
    val show = screen.point { calls(viewIfBound) }
        .next { invoke(Opcode.INVOKE_VIRTUAL, Opcode.INVOKE_INTERFACE) { paramCount(1); returns(Type.Void) } }
        .instruction.methodRef!!
    interfacesOf(show.definingClass)
        .first { host -> host.virtualMethods.any { it.name == show.name && it.proto == show.proto } }
        .virtualMethods.single { it.parameterTypes.isEmpty() && it.returnType == Type.Void }
}

private fun PatchRuntime.interfacesOf(type: String): List<DexClass> {
    val dexClass = bytecode.findClass(type) ?: return emptyList()
    return dexClass.interfaces.mapNotNull(bytecode::findClass) +
        (dexClass.interfaces + listOfNotNull(dexClass.superclass)).flatMap { interfacesOf(it) }
}
