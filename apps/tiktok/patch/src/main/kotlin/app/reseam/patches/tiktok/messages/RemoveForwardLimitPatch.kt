// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.tiktok.messages

import app.reseam.patch.Type
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.klass
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.tiktok.TikTokPatchStrings
import app.reseam.patches.tiktok.core.TIKTOK
import app.reseam.patches.tiktok.core.TikTokSettingsPages
import app.reseam.patches.tiktok.core.tiktokSettings
import app.reseam.tiktok.settings.MessageSettings
import app.reseam.tiktok.settings.TikTokStrings

// Every caller reads a negative limit as no limit.
private const val UNLIMITED = -1

val removeForwardLimit =
    patch(TikTokPatchStrings.removeForwardLimit) {
        description(TikTokPatchStrings.removeForwardLimitDescription)
        compatibleWith(TIKTOK)
        settings(
            tiktokSettings,
            section(
                TikTokSettingsPages.Inbox,
                TikTokStrings.sectionMessages,
                MessageSettings.removeForwardLimit,
            ),
        )

        execute {
            gate(MessageSettings.removeForwardLimit) {
                forwardLimits.all
                    .also {
                        check(it.size == 2) { "Expected two forward limits, found ${it.size}" }
                    }
                    .forEach { it.alwaysReturn(UNLIMITED) }
            }
        }
    }

private val forwardLimits =
    klass("com.ss.android.ugc.aweme.im.sharepanel.api.experiment.ForwardLimitSetting").methods(
        "forwardLimits"
    ) {
        flags(AccessFlags.STATIC)
        params(Type.Int)
        returns(Type.Int)
    }
