// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.instagram.profile

import app.reseam.instagram.settings.AppearanceSettings
import app.reseam.instagram.settings.InstagramStrings
import app.reseam.patch.after
import app.reseam.patch.dex.Opcode
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.instagram.InstagramPatchStrings
import app.reseam.patches.instagram.core.INSTAGRAM
import app.reseam.patches.instagram.core.instagramSettings
import app.reseam.patches.instagram.core.signatureCheck

private const val VIEW_GONE = 8

val hideProfileSuggestions = patch(InstagramPatchStrings.hideProfileSuggestions) {
    description(InstagramPatchStrings.hideProfileSuggestionsDescription)
    compatibleWith(INSTAGRAM)
    dependsOn(signatureCheck)
    settings(instagramSettings, section(InstagramStrings.sectionAppearance, AppearanceSettings.hideProfileSuggestions))

    execute {
        val containerId = resources.id("id", "similar_accounts_container")?.toLong() ?: error("id/similar_accounts_container missing")
        val container = method("profileHeaderHolder") {
            name("<init>")
            literals(containerId)
        }
            .point { literal(containerId) }
            .next { opcode(Opcode.IPUT_OBJECT) }
            .field("suggestionsContainer")
        val headerBinder = method("profileHeaderBinder") {
            strings("chaining_icon")
            hasParam(container.owner)
        }

        // Every bind of the profile header can show the container again.
        gate(AppearanceSettings.hideProfileSuggestions) {
            headerBinder.after {
                paramOfType(container.owner).field(container).callVirtual("android.view.View", "setVisibility", "(I)V", int(VIEW_GONE))
            }
        }
    }
}
