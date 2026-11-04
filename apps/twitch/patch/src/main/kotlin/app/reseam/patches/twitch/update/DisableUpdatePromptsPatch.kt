// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.twitch.update

import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.twitch.core.TWITCH
import app.reseam.patches.twitch.core.twitchSettings
import app.reseam.twitch.settings.TwitchStrings
import app.reseam.twitch.settings.UpdateSettings

private const val UPDATE_NOT_AVAILABLE = 1

val disableUpdatePrompts = patch("Disable update prompts") {
    description("Stops update prompts.")
    compatibleWith(TWITCH)
    settings(twitchSettings, section(TwitchStrings.sectionUpdates, UpdateSettings.disableUpdatePrompts))

    execute {
        gate(UpdateSettings.disableUpdatePrompts) {
            updateAvailability.after { capture("availability").assign(int(UPDATE_NOT_AVAILABLE)) }
        }
    }
}

private val updateAvailability = method("appUpdateInfoReply") { strings("update.availability", "nonblocking.intent") }
    .point { string("update.availability") }
    .next { resultOf(Type.Int) }
    .captureAs("availability", Type.Int)
