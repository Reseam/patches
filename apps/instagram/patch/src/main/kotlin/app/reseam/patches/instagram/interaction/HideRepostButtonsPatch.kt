// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.instagram.interaction

import app.reseam.patch.MethodTarget
import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.instagram.core.AppearanceSettings
import app.reseam.patches.instagram.core.INSTAGRAM
import app.reseam.patches.instagram.core.instagramSettings
import app.reseam.patches.instagram.core.signatureCheck

val hideRepostButtons = patch("Hide repost buttons") {
    description("Hides repost controls in Instagram's feed and reels.")
    compatibleWith(INSTAGRAM)
    dependsOn(signatureCheck)
    settings(instagramSettings, section("Appearance", AppearanceSettings.hideRepostButtons))

    execute {
        gate(AppearanceSettings.hideRepostButtons) {
            feedUfiConfig.method("<init>").after {
                thisObject.set(feedRepostEnabled, bool(false))
            }
            reelsRepostButtonFactory.alwaysReturnNull()
        }
        val repostCount = resources.id("id", "repost_count")?.toLong() ?: error("id/repost_count missing")
        gate(AppearanceSettings.hideRepostButtons) {
            method("reelsRepostCountFactory") {
                inClass(klass(reelsRepostButtonFactory.owner))
                literals(repostCount)
            }.alwaysReturnNull()
        }
    }
}

internal val feedUfiConfig = klass("feedUfiConfig") { strings(", isRepostButtonEnabled=") }

private val feedRepostEnabled = labelledBoolean(feedUfiConfig.method("toString"), ", isRepostButtonEnabled=")

private val reelsRepostIcon = method("reelsRepostIcon") { strings("clips_ufi_repost_button_component") }

private val reelsRepostButton = method("reelsRepostButton") { calls(klass(reelsRepostIcon.owner).method("<init>")) }

// The reels action rail builds the repost button and its count through two factories and already
// leaves either slot out when its factory returns null, as it does for media that cannot be reposted.
private val reelsRepostButtonFactory = method("reelsRepostButtonFactory") {
    calls(klass(reelsRepostButton.owner).method("<init>"))
    returns(reelsRepostButton.owner)
}

internal fun labelledBoolean(toString: MethodTarget, label: String) =
    toString.point { string(label); then(within = 6) { invokeVirtual { name("append"); params(Type.Boolean) } } }
        .writer(1)
        .field(label.trim(',', ' ', '='))
