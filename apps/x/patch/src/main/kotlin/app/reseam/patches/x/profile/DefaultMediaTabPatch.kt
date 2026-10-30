// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.x.profile

import app.reseam.patch.Type
import app.reseam.patch.before
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.x.core.X
import app.reseam.patches.x.core.xSettings
import app.reseam.x.profile.bindings.ProfileMedia
import app.reseam.x.settings.XSettings
import app.reseam.x.settings.XStrings

val defaultMediaTab = patch("Default media tab") {
    description("Opens a profile's Media tab on Photos or Videos.")
    compatibleWith(X)
    settings(xSettings, section(XStrings.sectionProfile, XSettings.defaultMediaTab))

    execute {
        combinedTimeline.before {
            val initial = paramOfType(profileSubTab.descriptor)
            initial.assign(ProfileMedia.initialSubTab(paramOfType(Type.List), initial).cast(profileSubTab.descriptor))
        }
    }
}

private val profileSubTab = klass("profileSubTab") { enumConstants("Posts", "Media", "Photos", "Videos") }

// A profile tab that groups sub-tabs, such as Media with Photos and Videos, opens on its initial sub-tab.
private val combinedTimeline = klass("combinedProfileTimeline") {
    strings("Combined timeline requires at least two sub-tabs")
}.method("<init>") { hasParam(Type.List); hasParam(profileSubTab.descriptor) }
