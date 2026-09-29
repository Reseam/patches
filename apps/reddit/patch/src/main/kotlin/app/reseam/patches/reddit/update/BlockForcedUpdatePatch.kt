// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.reddit.update

import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.reddit.core.REDDIT
import app.reseam.patches.reddit.core.RedditSettings
import app.reseam.patches.reddit.core.overrideStringConfigs
import app.reseam.patches.reddit.core.redditSettings

val blockForcedUpdate = patch("Block remote forced updates") {
    description("Stops Reddit from remotely blocking this version with a forced update screen.")
    compatibleWith(REDDIT)
    settings(redditSettings, section("Updates", RedditSettings.blockForcedUpdates))

    execute {
        overrideStringConfigs(RedditSettings.blockForcedUpdates, mapOf("android_disabled_build_numbers" to ""))
    }
}
