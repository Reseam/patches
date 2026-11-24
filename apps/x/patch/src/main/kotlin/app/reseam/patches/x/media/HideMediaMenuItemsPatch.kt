// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.x.media

import app.reseam.patch.after
import app.reseam.patch.dex.Opcode
import app.reseam.patch.dex.opcode
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.section
import app.reseam.patches.x.XPatchStrings
import app.reseam.patches.x.core.X
import app.reseam.patches.x.core.XSettingsPages
import app.reseam.patches.x.core.xSettings
import app.reseam.x.mediamenu.bindings.MediaMenu
import app.reseam.x.settings.XSettings
import app.reseam.x.settings.XStrings

val hideMediaMenuItems = patch(XPatchStrings.hideMediaMenuItems) {
    description(XPatchStrings.hideMediaMenuItemsDescription)
    compatibleWith(X)
    settings(
        xSettings,
        section(
            XSettingsPages.Media,
            XStrings.sectionMediaMenu,
            XSettings.hideMediaMenuPost,
            XSettings.hideMediaMenuOfflineVideos,
            XSettings.hideMediaMenuGrok,
        ),
    )

    execute {
        val postPhoto = resources.id("string", "post_photo")?.toLong() ?: error("string/post_photo missing")
        val addToOfflineVideos = resources.id("string", "add_to_offline_videos")?.toLong()
            ?: error("string/add_to_offline_videos missing")
        // The menu turns each action it iterates into a row; the list itself stays X's type.
        method("mediaMenu") { literals(postPhoto, addToOfflineVideos) }
            .point { invokeInterface { owner("java.lang.Iterable"); name("iterator") } }
            .captureArgumentAs("actions", 0)
            .next { opcode(Opcode.MOVE_RESULT_OBJECT) }
            .captureAs("rows", "Ljava/util/Iterator;")
            .after { capture("rows").assign(MediaMenu.visible(capture("actions"))) }
    }
}
