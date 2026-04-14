/*
 * Stitch — part of the Stitch Android patcher.
 * Copyright (C) 2026 Aunali321 <accounts@auna.li>
 *
 * This file is part of Stitch.
 *
 * Stitch is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Stitch is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package dev.stitch.patches.instagram.media.download

import dev.stitch.patch.compatibleWith
import dev.stitch.patch.patch
import dev.stitch.patch.settings.SettingsSection
import dev.stitch.patches.instagram.core.DownloadSettings
import dev.stitch.patches.instagram.core.settingsPatch
import dev.stitch.patches.instagram.refs.mediaRefs
import dev.stitch.patches.instagram.refs.userRefs

val downloadPatch = patch(
    name = "Download media",
    description = "Adds a download option to feed, reels, and story overflow menus.",
    compatibleWith = listOf(compatibleWith("com.instagram.android")),
    settingsHost = settingsPatch,
    dependsOn = listOf(settingsPatch, userRefs, mediaRefs),
    enabledByDefault = true,
    settings = listOf(
        SettingsSection(
            title = "Downloads",
            settings = listOf(
                DownloadSettings.Folder,
                DownloadSettings.ShowToast,
            ),
        ),
    ),
) {
    extendWith("instagram-download.dex")

    execute { ctx ->
        hookFeedMenuClick(ctx)
        hookFeedMenuItems(ctx)

        hookReelsMenuClick(ctx)
        hookLegacyReelsMenu(ctx)

        hookStoryMenu(ctx)

        hookOwnerBridges(ctx)
        hookMenuBridges(ctx)
    }
}
