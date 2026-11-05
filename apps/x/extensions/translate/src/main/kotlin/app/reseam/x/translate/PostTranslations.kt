// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("PostTranslations")

package app.reseam.x.translate

import app.reseam.runtime.Export
import app.reseam.x.api.register

@Export
fun init() {
    register(PostTranslator)
    register(TranslateButton)
}
