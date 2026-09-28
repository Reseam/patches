// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.instagram.interaction

import app.reseam.patch.MethodTarget
import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.after
import app.reseam.patch.settings.section
import app.reseam.patches.instagram.core.AppearanceSettings
import app.reseam.patches.instagram.core.INSTAGRAM
import app.reseam.patches.instagram.core.instagramSettings
import app.reseam.patches.instagram.core.signatureCheck

// Both surfaces already honour a flag of their own; forcing it keeps Instagram's layout intact.
val hideRepostButtons = patch("Hide repost buttons") {
    description("Hides repost controls in Instagram's feed and reels.")
    compatibleWith(INSTAGRAM)
    dependsOn(signatureCheck)
    settings(instagramSettings, section("Appearance", AppearanceSettings.hideRepostButtons))

    execute {
        feedUfiConfig.method("<init>").after(AppearanceSettings.hideRepostButtons) {
            thisObject.set(feedRepostEnabled, bool(false))
        }
        clipsViewerConfig.method("<init>").after(AppearanceSettings.hideRepostButtons) {
            thisObject.set(reelsHideRepost, bool(true))
        }
    }
}

private val feedUfiConfig = klass("feedUfiConfig") { strings(", isRepostButtonEnabled=") }

private val clipsViewerConfig = klass("com.instagram.clips.intf.ClipsViewerConfig")

private val feedRepostEnabled = labelledBoolean(feedUfiConfig.method("toString"), ", isRepostButtonEnabled=")

private val reelsHideRepost = labelledBoolean(clipsViewerConfig.method("toString"), ", hideReshareButton=")

private fun labelledBoolean(toString: MethodTarget, label: String) =
    toString.point { string(label); then(within = 6) { invokeVirtual { name("append"); params(Type.Boolean) } } }
        .writer(1)
        .field(label.trim(',', ' ', '='))
