// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.discord.core

import app.reseam.patch.function
import app.reseam.patch.patch
import app.reseam.patches.bundle.reactModule
import app.reseam.patches.bundle.reactNativeModule

/**
 * Declares the app modules Discord's extensions import. Each of Discord's own source files passes
 * its path to `fileFinishedImporting` from its Metro factory, so `discord:<path>` names the file
 * exactly, as in `discord:stores/UserStore.tsx`.
 */
internal val discordModules = patch {
    compatibleWith(DISCORD)

    execute {
        hermes.modules {
            module("react", reactModule)
            module(
                "react/jsx-runtime",
                function {
                    name("jsxProd")
                    paramCount(3)
                }.module,
            )
            module("react-native", reactNativeModule)
            prefix("discord:") { path ->
                function {
                    name("")
                    strings(path)
                    paramCount(7)
                }.module
            }
        }
    }
}
