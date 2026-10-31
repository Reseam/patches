// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.hide

import app.reseam.patch.PatchRuntime
import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.parameterTypes
import app.reseam.patch.dex.returnType
import app.reseam.patch.method
import app.reseam.patch.methods
import app.reseam.patch.point
import app.reseam.patch.settings.gate
import app.reseam.youtube.hidelayout.bindings.LayoutViews
import app.reseam.youtube.settings.FeedSettings

internal fun PatchRuntime.hideFeedFlyoutMenu() {
    val bottomSheetMenuItems = methods("bottomSheetMenuItems") {
        paramCount(1)
        strings("Text missing for BottomSheetMenuItem.")
        callsMethod { returnType == Type.CharSequence && parameterTypes.size == 1 && parameterTypes[0].startsWith("L") }
        custom { returnType.startsWith("L") }
    }
    bottomSheetMenuItems.forEach {
        gate(FeedSettings.hideFeedFlyoutMenu) {
            point("bottomSheetMenuText") {
                invokeStatic { returns(Type.CharSequence); paramCount(1) }
            }.next { resultOf(Type.CharSequence) }
                .captureAs("text", Type.CharSequence)
                .after {
                    capture("text").assign(LayoutViews.hideFlyoutMenu(capture("text")))
                }
        }
    }

    val posterArtWidthDefault = requireNotNull(resources.id("dimen", "poster_art_width_default")) {
        "dimen/poster_art_width_default is missing"
    }.toLong()
    val contextualMenuItem = method("contextualMenuItem") {
        flags(AccessFlags.PUBLIC or AccessFlags.FINAL or AccessFlags.SYNTHETIC)
        returns(Type.Void)
        paramCount(2)
        literals(posterArtWidthDefault)
        calls { owner("Landroid/widget/TextView;"); name("setText") }
    }
    gate(FeedSettings.hideFeedFlyoutMenu) {
        contextualMenuItem.point("contextualMenuText") {
            checkCast("Landroid/widget/TextView;")
            then(within = 5) {
                invokeVirtual { owner("Landroid/widget/TextView;"); name("setText"); params(Type.CharSequence) }
            }
        }
            .captureArgumentAs("textView", 0, TEXT_VIEW)
            .captureArgumentAs("text", 1, Type.CharSequence)
            .after {
                LayoutViews.hideFlyoutMenuForTablet(capture("textView"), capture("text"))
            }
    }
}
