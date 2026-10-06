// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.discord.core

import app.reseam.patch.CompatiblePackage
import app.reseam.patch.ExtClass
import app.reseam.patch.Type
import app.reseam.patch.invoke

val DISCORD: CompatiblePackage = "com.discord"("347.12 - Stable")

object DiscordSettingsEntry : ExtClass("app.reseam.discord.settings.DiscordSettingsEntry") {
    val init by static(Type.Context)
    val addReactPackage by static(Type.List)
}
