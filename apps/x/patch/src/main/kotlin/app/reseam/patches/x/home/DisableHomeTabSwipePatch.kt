// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.x.home

import app.reseam.patch.Type
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.x.core.X
import app.reseam.patches.x.core.xSettings
import app.reseam.x.settings.XSettings
import app.reseam.x.settings.XStrings

val disableHomeTabSwipe = patch("Disable Home tab swipe") {
    description("Stops swiping between the Home tabs. Tapping a tab still switches to it.")
    compatibleWith(X)
    settings(xSettings, section(XStrings.sectionHome, XSettings.disableHomeTabSwipe))

    execute {
        // Tab taps scroll the pager from code, which userScrollEnabled does not block.
        gate(XSettings.disableHomeTabSwipe) {
            homePager.point { calls(horizontalPager) }
                .captureArgumentAs("userScrollEnabled", horizontalPager.parameterTypes.indexOf(Type.Boolean))
                .before { capture("userScrollEnabled").assign(bool(false)) }
        }
    }
}

private val pagerState = klass("pagerState") { strings(" is not within the range -0.5 to 0.5") }

private val horizontalPager = method("horizontalPager") {
    param(0, pagerState.descriptor)
    hasParam(Type.Boolean)
    accesses { name("Horizontal") }
}

private val childPages = klass("childPages") { strings("ChildPages(items=") }

// Home's pager lambda keeps its ChildPages in a typed field; merged lambdas of other pagers store it as Object.
private val homePager = method("homePager") {
    calls(horizontalPager)
    accesses { type(childPages.descriptor) }
}
