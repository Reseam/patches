// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.twitch.core

import app.reseam.patch.function
import app.reseam.patch.patch

/** Declares the app modules that Twitch's JavaScript extensions import. */
internal val twitchModules = patch {
    compatibleWith(TWITCH)

    execute {
        hermes.modules {
            module(
                "react",
                function {
                    name("isValidElement")
                    strings("\$\$typeof")
                    paramCount(1)
                }.module,
            )
            module(
                "react-native",
                function {
                    name("")
                    strings("unstable_NativeText", "unstable_batchedUpdates")
                    paramCount(7)
                }.module,
            )
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
