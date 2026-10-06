// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.reddit.settings

import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.reddit.core.REDDIT
import app.reseam.patches.reddit.core.RedditSettings
import app.reseam.patches.reddit.core.SettingsRows
import app.reseam.patches.reddit.core.redditSettings
import app.reseam.patches.reddit.core.settingsItemKey
import app.reseam.patches.reddit.core.settingsItemRegistry

private const val SETTINGS_SECTION = "com.reddit.settings.usersettings.UserSettingsSection"

val hidePremiumSettings = patch("Hide Premium settings") {
    description("Removes Premium and Reddit Pro upsell rows from Reddit's settings, keeping app icons and your subscription row.")
    compatibleWith(REDDIT)
    settings(redditSettings, section("Settings", RedditSettings.hidePremiumSettings))

    execute {
        SettingsRows.section.implement { returnValue(param(0).cast(settingsItemSection.owner).call(settingsItemSection)) }
        SettingsRows.key.implement { returnValue(param(0).cast(settingsItemKey.owner).call(settingsItemKey)) }
        gate(RedditSettings.hidePremiumSettings) {
            settingsItemRegistry.before {
                param(0).assign(call(SettingsRows.withoutUpsells, param(0)))
            }
        }
    }
}

private val settingsItemSection = method("settingsItemSection") {
    inClass(klass(settingsItemKey.owner))
    returns(SETTINGS_SECTION)
}
