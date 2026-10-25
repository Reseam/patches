// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.hide

import app.reseam.patch.PatchRuntime
import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.dex.Opcode
import app.reseam.patch.dex.opcode
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.point
import app.reseam.patch.points
import app.reseam.patch.reserveLocal
import app.reseam.patch.settings.gate
import app.reseam.youtube.hidelayout.bindings.LayoutViews
import app.reseam.youtube.settings.ChannelSettings

internal fun PatchRuntime.hideChannelTabs() {
    val tabTitle = channelTabRenderer.point("channelTabTitle") {
        invoke { params(Type.String, Type.Boolean); returns(Type.CharSequence) }
    }.writer(1).field()
    // Older releases bind every tab in the loop over them, so the title is kept where it is read.
    val title = channelTabRenderer.reserveLocal("channelTabTitle", Type.String)
    channelTabRenderer.points("channelTabTitleReads") {
        opcode(Opcode.IGET_OBJECT)
        field { owner(tabTitle.owner); name(tabTitle.name) }
    }.forEach { captureAs("title", Type.String).after { local(title).assign(capture("title")) } }
    channelTabRenderer.points("channelTabViews") {
        invoke { returns(Type.View); hasParam(Type.CharSequence) }
    }.forEach {
        gate(ChannelSettings.hideChannelTab) {
            next { resultOf(Type.View) }.captureAs("tab", Type.View)
                .after {
                    LayoutViews.hideChannelTabView(capture("tab"), local(title))
                }
        }
    }
}

private val tabListController = klass("tabListController") {
    strings("TabRenderer.content contains SectionListRenderer but the tab does not have a section list controller.")
}

private val channelTabRenderer = method("channelTabRenderer") {
    inClass(tabListController)
    returns(Type.Void)
    calls { returns(Type.View); hasParam(Type.CharSequence) }
}
