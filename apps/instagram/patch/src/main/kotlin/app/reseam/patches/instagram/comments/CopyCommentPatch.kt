// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.instagram.comments

import app.reseam.instagram.comments.bindings.CommentCopy
import app.reseam.instagram.comments.bindings.CopyCommentClick
import app.reseam.instagram.settings.CommentSettings
import app.reseam.instagram.settings.InstagramStrings
import app.reseam.patch.Type
import app.reseam.patch.appEntry
import app.reseam.patch.dex.Opcode
import app.reseam.patch.dex.literal
import app.reseam.patch.dex.methodRef
import app.reseam.patch.dex.parameterTypes
import app.reseam.patch.dex.returnType
import app.reseam.patch.dex.typeRef
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.instagram.InstagramPatchStrings
import app.reseam.patches.instagram.core.INSTAGRAM
import app.reseam.patches.instagram.core.instagramSettings
import app.reseam.patches.instagram.core.signatureCheck

private const val COMMENT_REPOSITORY = "com.instagram.comments.mvvm.data.MediaCommentListRepository"
private const val FUNCTION0 = "kotlin.jvm.functions.Function0"

val copyComment = patch(InstagramPatchStrings.copyComment) {
    description(InstagramPatchStrings.copyCommentDescription)
    compatibleWith(INSTAGRAM)
    dependsOn(signatureCheck)
    settings(instagramSettings, section(InstagramStrings.sectionComments, CommentSettings.copyComment))

    execute {
        val icon = resources.id("drawable", "instagram_copy_pano_outline_24")?.toLong() ?: error("drawable/instagram_copy_pano_outline_24 missing")
        // Strings live in Instagram's string packs, so the label is the one its DM "Copy text" action pairs with this icon.
        val label = method("copyTextAction") { name("<init>"); paramCount(0); literals(icon) }
            .point { literal(icon) }.next { opcode(Opcode.CONST) }.instruction.literal!!

        // The builder looks the comment up first, then turns its actions into menu items, each a
        // (style, icon, label, click) item; Copy reuses the first item's types and style.
        val commentLookup = commentMenuBuilder.point { invokeStatic { paramCount(3); hasParam(Type.String) } }
        val comment = commentLookup.next { resultOf() }.captureAs("comment")
        val commentText = editComment.point { field { owner(commentLookup.instruction.methodRef!!.returnType); type(Type.String) } }.field("commentText")
        val firstItem = commentMenuBuilder.point { invokeDirect { name("<init>"); paramCount(4); hasParam(FUNCTION0) } }
        val itemConstructor = firstItem.instruction.methodRef!!
        val (iconType, labelType) = itemConstructor.parameterTypes.let { it[1] to it[2] }
        val itemType = firstItem.previous { opcode(Opcode.NEW_INSTANCE) }.instruction.typeRef!!
        val defaultStyle = firstItem.previous { opcode(Opcode.SGET_OBJECT) }.field("defaultMenuStyle")
        val items = comment.next { invokeDirect { owner(Type.ArrayList); name("<init>") } }
            .next { invokeDirect { owner(Type.ArrayList); name("<init>") } }
            .captureArgumentAs("items", 0)

        appEntry { CommentCopy.init(application) }
        CopyCommentClick.invoke.implement {
            CommentCopy.copy(thisObject.fieldOfType(Type.String))
            returnNull()
        }
        gate(CommentSettings.copyComment) {
            items.after {
                val click = newInstance(CopyCommentClick.descriptor, "(${Type.String})V", capture("comment").field(commentText))
                val copy = newInstance(
                    itemType,
                    "(${itemConstructor.parameterTypes.joinToString("")})V",
                    staticField(defaultStyle),
                    newInstance(iconType, "(I)V", int(icon.toInt())),
                    newInstance(labelType, "(I)V", int(label.toInt())),
                    click,
                )
                capture("items").callVirtual(Type.ArrayList, "add", "(${Type.Object})Z", copy)
            }
        }
    }
}

private val commentMenuBuilder = method("commentMenuBuilder") {
    accesses { type(COMMENT_REPOSITORY) }
    returns(Type.ArrayList)
    paramCount(6)
    param(2, Type.String)
    param(3, Type.String)
    param(4, Type.Boolean)
    param(5, Type.Boolean)
}

// Editing reads the comment's text and no other string of it.
private val editComment = klass("$COMMENT_REPOSITORY\$editComment\$1").method("invokeSuspend")
