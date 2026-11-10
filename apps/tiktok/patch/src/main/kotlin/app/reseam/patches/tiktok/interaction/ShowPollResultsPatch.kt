// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.tiktok.interaction

import app.reseam.patch.Type
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.tiktok.TikTokPatchStrings
import app.reseam.patches.tiktok.core.TIKTOK
import app.reseam.patches.tiktok.core.TikTokSettingsPages
import app.reseam.patches.tiktok.core.tiktokSettings
import app.reseam.tiktok.settings.InterfaceSettings
import app.reseam.tiktok.settings.TikTokStrings

private const val POLL_CELL = "com.ss.android.ugc.aweme.commentv2.commentlist.powercell.PollCell"
private const val POLL_INFO = "com.ss.android.ugc.aweme.comment.model.PollInfo"

val showPollResults =
    patch(TikTokPatchStrings.showPollResults) {
        description(TikTokPatchStrings.showPollResultsDescription)
        compatibleWith(TIKTOK)
        settings(
            tiktokSettings,
            section(
                TikTokSettingsPages.Video,
                TikTokStrings.sectionOverlay,
                InterfaceSettings.showPollResults,
            ),
        )

        execute {
            gate(InterfaceSettings.showPollResults) { showsResults.alwaysReturn(true) }
        }
    }

private val showsResults =
    method("showsResults") {
        inClass(klass(POLL_CELL))
        params(POLL_INFO)
        returns(Type.Boolean)
        calls {
            owner(POLL_CELL)
            params(POLL_INFO)
            returns(Type.Boolean)
        }
    }
