// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.x.translate

import app.reseam.patch.appEntry
import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.x.XPatchStrings
import app.reseam.patches.x.core.X
import app.reseam.patches.x.core.XSettingsPages
import app.reseam.patches.x.core.xApi
import app.reseam.patches.x.core.xSettings
import app.reseam.patches.x.featureswitches.featureSwitchOverrides
import app.reseam.x.settings.XSettings
import app.reseam.x.settings.XStrings
import app.reseam.x.translate.bindings.PostTranslations

val translatePosts = patch(XPatchStrings.translatePosts) {
    description(XPatchStrings.translatePostsDescription)
    compatibleWith(X)
    dependsOn(xApi, featureSwitchOverrides)
    settings(xSettings, section(XSettingsPages.Grok, XStrings.sectionTranslation, XSettings.postTranslator))

    execute {
        appEntry { PostTranslations.init() }
    }
}
