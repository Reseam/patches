// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.instagram.stories

import app.reseam.patch.Type
import app.reseam.patch.dex.Opcode
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.instagram.core.INSTAGRAM
import app.reseam.patches.instagram.core.StorySettings
import app.reseam.patches.instagram.core.StorySuggestions
import app.reseam.patches.instagram.core.instagramSettings
import app.reseam.patches.instagram.core.signatureCheck

val hideStorySuggestions = patch("Hide suggested accounts in stories") {
    description("Removes stories and accounts you don't follow from the story tray, and the suggested-account cards between stories.")
    compatibleWith(INSTAGRAM)
    dependsOn(signatureCheck)
    settings(instagramSettings, section("Stories", StorySettings.hideSuggestedUsers))

    execute {
        gate(StorySettings.hideSuggestedUsers) {
            midcardFetch.alwaysReturn()
            trayEligibility.before {
                whenTrue(call(StorySuggestions.isSuggested, param(0).field(reelType))) { returnFalse() }
            }
        }
    }
}

private val midcardFetch = method("midcardFetch") {
    strings("ReelStore.fetchClientInjectedMidcards")
    returns(Type.Void)
}

// Every tray list, and so the viewer's paging, keeps only the reels this accepts.
private val trayEligibility = method("trayEligibility") {
    strings("If reel.isBroadcastReel(), then reel.getReelBroadcastItem() cannot be null")
    returns(Type.Boolean)
}

private val reelType = trayEligibility.point { opcode(Opcode.IGET_OBJECT) }.field("reelType")
