// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtubemusic.core

import app.reseam.patch.settings.SettingsPage

internal object YouTubeMusicSettingsPages {
    val Playback = SettingsPage("playback", "Playback", order = 0)
    val Player = SettingsPage("player", "Player", order = 10)
    val Ads = SettingsPage("ads", "Ads", order = 20)
    val Interface = SettingsPage("interface", "Interface", order = 30)
    val Advanced = SettingsPage("advanced", "Advanced", order = 40)

    val Menus = SettingsPage("interface.menus", "Menus", Interface, order = 0)
}
