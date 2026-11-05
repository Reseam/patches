// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.x.timeline

import app.reseam.patch.appEntry
import app.reseam.patch.before
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.x.core.X
import app.reseam.patches.x.core.xSettings
import app.reseam.x.settings.XSettings
import app.reseam.x.settings.XStrings
import app.reseam.x.withheld.bindings.WithheldPosts

val showWithheldPosts = patch("Show withheld posts") {
    description("Shows posts and profiles withheld in your country.")
    compatibleWith(X)
    settings(xSettings, section(XStrings.sectionTimeline, XSettings.showWithheldPosts))

    execute {
        appEntry { WithheldPosts.init(application) }

        // Every GraphQL client is an OkHttp client, and OkHttp keeps its names, so one interceptor
        // sees each response before Apollo parses it, on every screen.
        clientBuild.before { WithheldPosts.install(thisObject) }
    }
}

private val clientBuild = klass($$"okhttp3.OkHttpClient$Builder").method("build")
