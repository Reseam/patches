// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.bundle

import app.reseam.patch.JsCodeScope
import app.reseam.patch.JsValue
import app.reseam.patch.function

val reactModule = function {
    name("isValidElement")
    strings("\$\$typeof")
    paramCount(1)
}.module

val reactNativeModule = function {
    name("")
    strings("unstable_NativeText", "unstable_batchedUpdates")
    paramCount(7)
}.module

/** The exports object of the Metro module whose factory this hook runs in, its sixth parameter. */
val JsCodeScope.moduleExports: JsValue
    get() = param(5)
