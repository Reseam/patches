// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.tiktok.interaction

import app.reseam.patch.Type
import app.reseam.patch.appEntry
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patch.settings.whenEnabled
import app.reseam.patches.tiktok.core.TIKTOK
import app.reseam.patches.tiktok.core.TikTokSettingsPages
import app.reseam.patches.tiktok.core.tiktokSettings
import app.reseam.tiktok.interaction.bindings.Shortcuts
import app.reseam.tiktok.settings.InterfaceSettings
import app.reseam.tiktok.settings.TikTokStrings

private val shortcutManager = klass("com.ss.android.ugc.aweme.shortcut.TiktokShortcutManager")

val removeAppShortcuts =
    patch("Remove app shortcuts") {
        description("Removes TikTok's entries from the long-press menu of its launcher icon.")
        compatibleWith(TIKTOK)
        settings(
            tiktokSettings,
            section(
                TikTokSettingsPages.SearchAndApp,
                TikTokStrings.sectionApp,
                InterfaceSettings.removeAppShortcuts,
            ),
        )

        execute {
            gate(InterfaceSettings.removeAppShortcuts) {
                publishShortcuts.alwaysReturn()
                refreshShortcuts.alwaysReturn()
            }
            // Shortcuts published before the setting was turned on stay until removed.
            appEntry {
                whenEnabled(InterfaceSettings.removeAppShortcuts) {
                    call(Shortcuts.removeAll, application)
                }
            }
        }
    }

private val publishShortcuts =
    method("publishShortcuts") {
        inClass(shortcutManager)
        params(Type.String, Type.Boolean, Type.List)
        returns(Type.Void)
    }

private val refreshShortcuts =
    method("refreshShortcuts") {
        inClass(shortcutManager)
        strings("refreshIfNotWhenColdBoot, ")
        returns(Type.Void)
    }
