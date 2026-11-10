// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.tiktok.interaction

import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.field
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.tiktok.TikTokPatchStrings
import app.reseam.patches.tiktok.core.AWEME
import app.reseam.patches.tiktok.core.TIKTOK
import app.reseam.patches.tiktok.core.TikTokSettingsPages
import app.reseam.patches.tiktok.core.tiktokSettings
import app.reseam.tiktok.settings.InterfaceSettings
import app.reseam.tiktok.settings.TikTokStrings

private const val VIDEO_CONTROL = "com.ss.android.ugc.aweme.feed.model.VideoControl"
private const val SHOWN = 1
private const val VISIBLE = 0

val alwaysShowSeekBar =
    patch(TikTokPatchStrings.alwaysShowSeekBar) {
        description(TikTokPatchStrings.alwaysShowSeekBarDescription)
        compatibleWith(TIKTOK)
        settings(
            tiktokSettings,
            section(
                TikTokSettingsPages.Video,
                TikTokStrings.sectionOverlay,
                InterfaceSettings.alwaysShowSeekBar,
            ),
        )

        execute {
            // The feed seek bar shows only when the server marks a video both draggable and
            // visible.
            val flags =
                listOf("draftProgressBar", "showProgressBar").map(klass(VIDEO_CONTROL)::field)
            gate(InterfaceSettings.alwaysShowSeekBar) {
                videoControl.after {
                    whenNotNull(result) { flags.forEach { result.set(it, int(SHOWN)) } }
                }
                seekBarStyle.alwaysReturn(VISIBLE)
            }
        }
    }

private val videoControl = klass(AWEME).method("getVideoControl") { returns(VIDEO_CONTROL) }

// Picks the bar's style while a video plays: visible from 30 seconds, otherwise transparent until
// touched.
private val seekBarStyle =
    method("seekBarStyle") {
        inClass(
            klass("seekBarController") { strings("can not show seekbar, state: 2, can not drag") }
        )
        params(Type.Boolean)
        returns(Type.Int)
        literals(30, 502)
    }
