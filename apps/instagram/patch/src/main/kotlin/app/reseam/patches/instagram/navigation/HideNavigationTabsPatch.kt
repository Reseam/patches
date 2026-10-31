// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.instagram.navigation

import app.reseam.instagram.navigation.bindings.NavigationTabs
import app.reseam.instagram.settings.AppearanceSettings
import app.reseam.instagram.settings.InstagramStrings
import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patch.settings.valueOf
import app.reseam.patches.instagram.core.INSTAGRAM
import app.reseam.patches.instagram.core.USER_SESSION
import app.reseam.patches.instagram.core.instagramSettings
import app.reseam.patches.instagram.core.signatureCheck

val hideNavigationTabs = patch("Hide navigation tabs") {
    description("Removes the Reels or Create tab from the bottom bar.")
    compatibleWith(INSTAGRAM)
    dependsOn(signatureCheck)
    settings(instagramSettings, section(InstagramStrings.sectionAppearance, AppearanceSettings.hideReelsTab, AppearanceSettings.hideCreateTab))

    execute {
        tabListBuilder.after {
            returnValue(NavigationTabs.filter(result, valueOf(AppearanceSettings.hideReelsTab), valueOf(AppearanceSettings.hideCreateTab)))
        }
    }
}

private val tabEnum = klass("tabEnum") { strings("fragment_clips", "clips_viewer_clips_tab", "fragment_direct_tab") }

private val tabListBuilder = method("tabListBuilder") {
    returns(Type.List)
    params(USER_SESSION, Type.Boolean)
    calls { owner(tabEnum.descriptor); params(Type.String); returns(tabEnum.descriptor) }
}
