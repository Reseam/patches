// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.x.timeline

import app.reseam.patch.appEntry
import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.x.XPatchStrings
import app.reseam.patches.x.core.X
import app.reseam.patches.x.core.XSettingsPages
import app.reseam.patches.x.core.xApi
import app.reseam.patches.x.core.xSettings
import app.reseam.x.settings.XSettings
import app.reseam.x.settings.XStrings
import app.reseam.x.withheld.bindings.WithheldPosts

val showWithheldPosts = patch(XPatchStrings.showWithheldPosts) {
    description(XPatchStrings.showWithheldPostsDescription)
    compatibleWith(X)
    dependsOn(xApi)
    settings(xSettings, section(XSettingsPages.Home, XStrings.sectionTimeline, XSettings.showWithheldPosts))

    execute {
        appEntry { WithheldPosts.init(application) }
    }
}
