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
            // The icon's module also exports it wrapped as IconRefresh, the way the player draws its buttons.
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
