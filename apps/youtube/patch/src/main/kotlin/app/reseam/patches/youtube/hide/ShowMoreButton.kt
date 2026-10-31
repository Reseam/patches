// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.hide

import app.reseam.patch.FieldTarget
import app.reseam.patch.PatchRuntime
import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.classTarget
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.Opcode
import app.reseam.patch.dex.opcode
import app.reseam.patch.method
import app.reseam.patch.point
import app.reseam.patch.settings.gate
import app.reseam.youtube.hidelayout.bindings.LayoutViews
import app.reseam.youtube.settings.FeedSettings

internal fun PatchRuntime.hideShowMoreButton() {
    val linkTextStart = requireNotNull(resources.id("id", "link_text_start")) {
        "id/link_text_start is missing"
    }.toLong()
    val expandButtonContainer = requireNotNull(resources.id("id", "expand_button_container")) {
        "id/expand_button_container is missing"
    }.toLong()
    val showMoreSetView = method("hideShowMoreButtonSetView") {
        returns(Type.Void)
        literals(linkTextStart, expandButtonContainer)
    }
    val showMoreClass = classTarget("showMoreClass") {
        requireNotNull(bytecode.findClass(showMoreSetView.owner)) { "The Show more class is missing" }
    }
    val textViewField = fieldAfterLiteral(showMoreSetView, linkTextStart, "Landroid/widget/TextView;")
    val buttonContainerField = fieldAfterLiteral(showMoreSetView, expandButtonContainer, VIEW_DESCRIPTOR)
    val showMoreMethod = method("hideShowMoreButton") {
        inClass(showMoreClass)
        returns(Type.Void)
        paramCount(2)
        calls { owner(VIEW_DESCRIPTOR); name("setContentDescription"); params(Type.CharSequence) }
    }
    val parentViewMethod = method("showMoreParentView") {
        inClass(showMoreClass)
        flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
        params()
        returns(Type.View)
    }
    gate(FeedSettings.hideShowMoreButton) {
        showMoreMethod.after {
            LayoutViews.hideShowMoreButton(thisObject.call(parentViewMethod).cast(Type.View), thisObject.field(buttonContainerField).cast(Type.View), thisObject.field(textViewField).cast(TEXT_VIEW))
        }
    }
}

private fun fieldAfterLiteral(target: app.reseam.patch.MethodTarget, literal: Long, type: String): FieldTarget =
    target.point("fieldAfterResource_$literal") { literal(literal) }
        .next {
            opcode(Opcode.IPUT_OBJECT)
            field {
                owner(target.owner)
                type(type)
            }
        }
        .field()
