// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.twitch.core

import app.reseam.patch.function
import app.reseam.patch.patch
import app.reseam.patches.bundle.reactModule
import app.reseam.patches.bundle.reactNativeModule

/** Declares the app modules that Twitch's JavaScript extensions import. */
internal val twitchModules = patch {
    compatibleWith(TWITCH)

    execute {
        hermes.modules {
            module("react", reactModule)
            module("react-native", reactNativeModule)
            // The module also exports the wrapped IconRefresh that the player draws.
            module(
                "twitch:icons/IconRefresh",
                function {
                    name("RawIconRefresh")
                    paramCount(1)
                }.module,
            )
        }
    }
}
