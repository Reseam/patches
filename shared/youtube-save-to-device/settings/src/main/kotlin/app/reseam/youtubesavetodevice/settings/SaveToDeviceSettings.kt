// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtubesavetodevice.settings

import app.reseam.patch.settings.SettingsGroup
import app.reseam.patch.settings.folder
import app.reseam.patch.settings.toggle

object SaveToDeviceSettings : SettingsGroup(SaveToDeviceStrings) {
    val saveFolder by folder(default = "")
    val saveNewDownloads by toggle(default = false)
}
