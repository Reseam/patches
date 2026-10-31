// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.discord.core

import app.reseam.patch.JsCodeScope
import app.reseam.patch.JsValue
import app.reseam.patch.function
import app.reseam.patch.patch

/**
 * Declares the app modules Discord's extensions import. Each of Discord's own source files passes
 * its path to `fileFinishedImporting` from its Metro factory, so `discord:<path>` names the file
 * exactly, as in `discord:stores/UserStore.tsx`.
 */
internal val discordModules = patch {
    compatibleWith(DISCORD)

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
                "react/jsx-runtime",
                function {
                    name("jsxProd")
                    paramCount(3)
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

/** The exports object of the Metro module whose factory this hook runs in, its sixth parameter. */
internal val JsCodeScope.moduleExports: JsValue
    get() = param(5)
