// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("WithheldPosts")

package app.reseam.x.withheld

import android.content.Context
import app.reseam.runtime.Export
import app.reseam.x.api.register

@Export
fun init(context: Context) {
    register(WithheldRewriter)
    register(RefusedRepostNotice(context.applicationContext))
}
