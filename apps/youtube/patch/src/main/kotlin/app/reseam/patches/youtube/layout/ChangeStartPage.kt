// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.layout

import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.before
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.youtube.misc.bindings.StartPage
import app.reseam.youtube.settings.AppearanceSettings
import app.reseam.youtube.settings.YouTubeStrings

private val paneDescriptor = klass("paneDescriptor") { strings("PaneDescriptor for ") }

val changeStartPage = patch("Change start page") {
    description("Opens a page of your choice instead of the home feed.")
    compatibleWith(YOUTUBE)
    settings(
        youTubeSettings,
        section(YouTubeSettingsPages.Appearance, YouTubeStrings.sectionStartPage, AppearanceSettings.changeStartPage, AppearanceSettings.changeStartPageAlways),
    )

    execute {
        homeBrowseId.after { capture("browseId").assign(StartPage.overrideBrowseId(capture("browseId"))) }
        // Shorts and Search have no browse id of their own, so they are reached by intent action.
        launchIntent.before { StartPage.overrideIntentAction(param(0)) }
    }
}

// The home pane is the one place the default browse id is written into a feed request.
val homePaneDescriptor = method("homePaneDescriptor") {
    strings("FEwhat_to_watch")
    literals(512L)
    returns(paneDescriptor.descriptor)
}

val homeBrowseId = homePaneDescriptor
    .point { string("FEwhat_to_watch") }
    .captureAs("browseId", Type.String)

// The activity records that it handled the launch intent, so this is where the intent still is.
val launchIntent = method("launchIntent") {
    strings("has_handled_intent")
    params("android.content.Intent")
}
