// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later


package app.reseam.patches.instagram.media.download

import app.reseam.patch.compatibleWith
import app.reseam.patch.patch
import app.reseam.patch.settings.SettingsSection
import app.reseam.patches.instagram.core.DownloadSettings
import app.reseam.patches.instagram.core.settingsPatch

val downloadPatch = patch(
    name = "Download media",
    description = "Adds a download option to feed, reels, and story overflow menus.",
    compatibleWith = listOf(compatibleWith("com.instagram.android")),
    settingsHost = settingsPatch,
    dependsOn = listOf(settingsPatch),
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

        hookUrlBridges(ctx)
        hookOwnerBridges(ctx)
        hookListenerBridges(ctx)
        hookMenuBridges(ctx)
    }
}
