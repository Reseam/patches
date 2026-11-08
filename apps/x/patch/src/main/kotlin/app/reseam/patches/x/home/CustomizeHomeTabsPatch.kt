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
import app.reseam.x.home.bindings.HomeTabs
import app.reseam.x.settings.XSettings
import app.reseam.x.settings.XStrings

val customizeHomeTabs = patch("Customize Home tabs") {
    description("Hides the For You tab and the Add tab above Home.")
    compatibleWith(X)
    settings(xSettings, section(XStrings.sectionHome, XSettings.hideForYouTab, XSettings.hideAddTab))

    execute {
        HomeTabs.isForYou.implement {
            val type = param(0).cast(homeTab.descriptor).fieldOfType(initialHomeTab.returnType)
            whenInstanceOf(type, forYouHomeTab.descriptor) { returnValue(bool(true)) }
            returnValue(bool(false))
        }
        // For You and Following are the fixed tabs every page list starts from: the first pages, the
        // start tab lookup and each rebuild with pinned tabs.
        gate(XSettings.hideForYouTab) {
            homeTabbed.point { type("[${homeTab.descriptor}") }
                .next { invokeStatic { params("[${Type.Object}") } }
                .captureArgumentAs("tabs", 0)
                .before { capture("tabs").assign(HomeTabs.withoutForYou(capture("tabs"))) }
        }
        gate(XSettings.hideAddTab) {
            homeTabsUpdate.point { field { type(topicsUpsellHomeTab.descriptor) } }
                .next { invokeInterface { name("add") } }
                .skip()
            // Features(showTimelineSettings, showAddTabButton, showTrailingAddButton, ...): the + after the tabs.
            homeFeatures.before { param(2).assign(bool(false)) }
        }
    }
}

private val homeTab = klass("homeTab") { strings("Tab(homeTabType=") }

private val forYouHomeTab = klass("forYouHomeTab") {
    strings("ForYou")
    extends(initialHomeTab.returnType)
}

private val topicsUpsellHomeTab = klass("topicsUpsellHomeTab") {
    strings("TopicsUpsell")
    extends(initialHomeTab.returnType)
}

private val homeFeatures = klass("homeFeatures") { strings("Features(showTimelineSettings=") }.method("<init>")

private val homeTabbed = method("homeTabbed") { calls(homeFeatures) }

private val homeTabsUpdate = method("homeTabsUpdate") {
    calls { owner(homeTab.descriptor); name("<init>") }
    accesses { type(topicsUpsellHomeTab.descriptor) }
}
