// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.reddit.update

import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.reddit.core.REDDIT
import app.reseam.reddit.settings.RedditSettings
import app.reseam.patches.reddit.core.overrideStringConfigs
import app.reseam.patches.reddit.core.redditSettings
import app.reseam.reddit.settings.RedditStrings

val blockForcedUpdate = patch("Block remote forced updates") {
    description("Stops Reddit from remotely blocking this version with a forced update screen.")
    compatibleWith(REDDIT)
    settings(redditSettings, section(RedditStrings.sectionUpdates, RedditSettings.blockForcedUpdates))

    execute {
        overrideStringConfigs(RedditSettings.blockForcedUpdates, mapOf("android_disabled_build_numbers" to ""))
    }
}
