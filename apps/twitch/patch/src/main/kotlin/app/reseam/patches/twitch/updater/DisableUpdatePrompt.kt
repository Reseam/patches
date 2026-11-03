// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.twitch.updater

import app.reseam.patch.Type
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.twitch.core.TWITCH_PACKAGE
import app.reseam.patches.twitch.core.twitchSettings
import app.reseam.twitch.settings.TwitchStrings
import app.reseam.twitch.settings.UpdateSettings

// Google Play's UpdateAvailability.UPDATE_NOT_AVAILABLE
private const val UPDATE_NOT_AVAILABLE = 1

// Reads Google Play's answer to whether an update is available, which decides Twitch's update prompt
val appUpdateInfoReceiver = method("appUpdateInfoReceiver") {
    strings("update.availability", "nonblocking.intent")
}

val updateAvailability = appUpdateInfoReceiver.point("updateAvailability") {
    string("update.availability")
}.next { resultOf(Type.Int) }

@Suppress("unused")
val disableUpdatePrompt = patch("Disable update prompt") {
    description("Stops the prompt that offers a newer version from Google Play.")
    compatibleWith(TWITCH_PACKAGE)
    settings(twitchSettings, section(TwitchStrings.sectionUpdates, UpdateSettings.disableUpdatePrompt))

    execute {
        gate(UpdateSettings.disableUpdatePrompt) {
            updateAvailability.captureAs("availability").after {
                capture("availability").assign(int(UPDATE_NOT_AVAILABLE))
            }
        }
    }
}
