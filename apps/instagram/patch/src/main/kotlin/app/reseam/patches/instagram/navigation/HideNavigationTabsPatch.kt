// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.instagram.navigation

import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.ReseamSettings
import app.reseam.patch.settings.ToggleSetting
import app.reseam.patch.settings.section
import app.reseam.patches.instagram.core.AppearanceSettings
import app.reseam.patches.instagram.core.INSTAGRAM
import app.reseam.patches.instagram.core.NavigationTabs
import app.reseam.patches.instagram.core.USER_SESSION
import app.reseam.patches.instagram.core.instagramSettings
import app.reseam.patches.instagram.core.signatureCheck

val hideNavigationTabs = patch("Hide navigation tabs") {
    description("Removes the Reels or Create tab from the bottom bar.")
    compatibleWith(INSTAGRAM)
    dependsOn(signatureCheck)
    settings(instagramSettings, section("Appearance", AppearanceSettings.hideReelsTab, AppearanceSettings.hideCreateTab))

    execute {
        tabListBuilder.after {
            fun enabled(setting: ToggleSetting) = call(ReseamSettings.getBoolean, string(setting.key), bool(setting.default))
            returnValue(call(NavigationTabs.filter, capture("result"), enabled(AppearanceSettings.hideReelsTab), enabled(AppearanceSettings.hideCreateTab)))
        }
    }
}

private val tabEnum = klass("tabEnum") { strings("fragment_clips", "clips_viewer_clips_tab", "fragment_direct_tab") }

private val tabListBuilder = method("tabListBuilder") {
    returns(Type.List)
    params(USER_SESSION, Type.Boolean)
    calls { owner(tabEnum.descriptor); params(Type.String); returns(tabEnum.descriptor) }
}
