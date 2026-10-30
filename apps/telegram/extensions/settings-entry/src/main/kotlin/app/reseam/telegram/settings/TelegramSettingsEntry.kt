// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("TelegramSettingsEntry")

package app.reseam.telegram.settings

import android.app.Activity
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.View
import app.reseam.bundle.ReseamPatches
import app.reseam.runtime.Export

@Volatile private var cachedLogo: Drawable? = null

// Built from Telegram's own cell and list item by the settings host.
@Export external fun reseamItem(activity: Activity): Any?

@Export
fun opener(): View.OnClickListener = View.OnClickListener { ReseamPatches.bundle().openSettings() }

@Export
fun logo(ctx: Context): Drawable? =
    cachedLogo
        ?: runCatching {
            ctx.assets.open("reseam-patches/logo.png").use { input ->
                BitmapFactory.decodeStream(input)?.let {
                    BitmapDrawable(ctx.resources, it).also { logo -> cachedLogo = logo }
                }
            }
        }
            .getOrNull()
