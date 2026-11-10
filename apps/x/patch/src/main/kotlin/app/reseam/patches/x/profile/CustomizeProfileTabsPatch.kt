// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.x.profile

import app.reseam.patch.Type
import app.reseam.patch.before
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.x.XPatchStrings
import app.reseam.patches.x.core.X
import app.reseam.patches.x.core.XSettingsPages
import app.reseam.patches.x.core.xSettings
import app.reseam.x.profile.bindings.ProfileTabs
import app.reseam.x.settings.XSettings
import app.reseam.x.settings.XStrings

val customizeProfileTabs = patch(XPatchStrings.customizeProfileTabs) {
    description(XPatchStrings.customizeProfileTabsDescription)
    compatibleWith(X)
    settings(
        xSettings,
        section(
            XSettingsPages.Profile,
            XStrings.sectionProfile,
            XSettings.hideRepliesTab,
            XSettings.hideHighlightsTab,
            XSettings.hideArticlesTab,
            XSettings.hideMediaTab,
            XSettings.hideRepostsTab,
            XSettings.hideAffiliatesTab,
            XSettings.hideSubscriptionsTab,
            XSettings.hideLikesTab,
        ),
    )

    execute {
        ProfileTabs.subTab.implement {
            returnValue(param(0).cast(profileTab.descriptor).fieldOfType(profileSubTab.descriptor))
        }
        profileTabs.point { type("[${profileTab.descriptor}") }
            .next { invokeStatic { params("[${Type.Object}") } }
            .captureArgumentAs("tabs", 0)
            .before { capture("tabs").assign(ProfileTabs.visible(capture("tabs"))) }
        // With x_lite_profile_combined_posts_highlights_enabled, Highlights is a sub-tab grouped into Posts.
        gate(XSettings.hideHighlightsTab) {
            profileTabs.point {
                field { owner(profileSubTab.descriptor); name("Highlights") }
                then { invokeInterface { name("add") } }
            }.skip()
        }
    }
}

private val profileTab = klass("profileTab") { strings("ProfileTab(type=") }

private val profileTabs = method("profileTabs") { strings("x_lite_profile_combined_posts_highlights_enabled") }
