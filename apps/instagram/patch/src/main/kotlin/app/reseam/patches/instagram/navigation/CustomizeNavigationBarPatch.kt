// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.instagram.navigation

import app.reseam.instagram.navigation.bindings.NavigationBar
import app.reseam.instagram.settings.InstagramStrings
import app.reseam.instagram.settings.NavigationSettings
import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.instagram.InstagramPatchStrings
import app.reseam.patches.instagram.core.INSTAGRAM
import app.reseam.patches.instagram.core.USER_SESSION
import app.reseam.patches.instagram.core.instagramSettings
import app.reseam.patches.instagram.core.signatureCheck

val customizeNavigationBar = patch(InstagramPatchStrings.customizeNavigationBar) {
    description(InstagramPatchStrings.customizeNavigationBarDescription)
    compatibleWith(INSTAGRAM)
    dependsOn(signatureCheck)
    settings(
        instagramSettings,
        section(
            InstagramStrings.sectionNavigation,
            NavigationSettings.firstTab,
            NavigationSettings.secondTab,
            NavigationSettings.thirdTab,
            NavigationSettings.fourthTab,
            NavigationSettings.fifthTab,
        ),
    )

    execute {
        tabListBuilder.after {
            returnValue(NavigationBar.arrange(result))
        }
        landingTab.after {
            returnValue(NavigationBar.landingTab(result).cast(tabEnum.descriptor))
        }
    }
}

private val tabEnum = klass("tabEnum") {
    extends("java.lang.Enum")
    strings("fragment_clips", "clips_viewer_clips_tab", "fragment_direct_tab")
}

private val tabListBuilder = method("tabListBuilder") {
    returns(Type.List)
    params(USER_SESSION, Type.Boolean)
    calls { owner(tabEnum.descriptor); params(Type.String); returns(tabEnum.descriptor) }
}

private val landingTab = method("landingTab") {
    returns(tabEnum.descriptor)
    params(USER_SESSION)
    calls { params(USER_SESSION); returns(tabEnum.descriptor) }
}
