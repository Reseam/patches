// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.reddit.core

import app.reseam.patch.appEntry
import app.reseam.patch.settings.settingsHost

val redditSettings = settingsHost("reddit") {
    compatibleWith(REDDIT)

    install {
        appEntry {
            call(RedditSettingsEntry.init, application)
        }
    }
}
