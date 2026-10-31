// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.hide

import app.reseam.patch.PatchRuntime
import app.reseam.patch.after
import app.reseam.patch.method
import app.reseam.patch.settings.gate
import app.reseam.patches.youtubecommon.emptyComponentBuilder
import app.reseam.patches.youtubecommon.emptyComponentField
import app.reseam.youtube.hidelayout.bindings.LayoutComponentsFilter
import app.reseam.youtube.settings.FeedSettings

internal fun PatchRuntime.hideMixPlaylists() {
    gate(FeedSettings.hideMixPlaylists) {
        parseElementFromBuffer.after {
            whenTrue(LayoutComponentsFilter.filterMixPlaylists(param(1), param(2))) {
                returnValue(call(emptyComponentBuilder, param(0)).field(emptyComponentField))
            }
        }
    }
}

private val parseElementFromBuffer = method("parseElementFromBuffer") {
    paramCount(5)
    param(2, "[B")
    strings("Failed to parse Element.")
}
