// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.player

import app.reseam.bundle.Logger
import app.reseam.runtime.Export
import app.reseam.youtube.core.debug
import java.util.concurrent.atomic.AtomicReference

object EngagementPanel {
    private val panelId = AtomicReference("")

    val openPanelId: String
        get() = panelId.get()

    @Export
    @JvmStatic
    fun open(panelId: String?) {
        if (panelId.isNullOrEmpty()) return
        this.panelId.set(panelId)
        Logger.debug { "Engagement panel opened: $panelId" }
    }

    @Export
    @JvmStatic
    fun close() {
        val panelId = this.panelId.getAndSet("")
        if (panelId.isNotEmpty()) Logger.debug { "Engagement panel closed: $panelId" }
    }
}
