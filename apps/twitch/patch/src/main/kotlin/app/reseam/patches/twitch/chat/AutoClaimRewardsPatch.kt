// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.twitch.chat

import app.reseam.patch.Type
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.twitch.core.TWITCH
import app.reseam.patches.twitch.core.twitchSettings
import app.reseam.twitch.chat.bindings.AutoClaim
import app.reseam.twitch.settings.ChatSettings
import app.reseam.twitch.settings.TwitchStrings

private const val COMMUNITY_POINTS_MODEL = "Ltv/twitch/android/models/communitypoints/CommunityPointsModel;"

private val claimMutation = klass("claimMutation") { strings("ClaimCommunityPointsMutation") }

private val claimBonus = method("claimBonus") {
    calls { owner(claimMutation.descriptor); name("<init>") }
    params(Type.String, "Ltv/twitch/android/shared/one/chat/pub/ChatModeMetadata;")
}

private val pointsUpdate = method("pointsUpdate") {
    accesses {
        owner(claimBonus.owner)
        type(COMMUNITY_POINTS_MODEL)
    }
    params(Type.Object)
}

private val activeClaim = klass(COMMUNITY_POINTS_MODEL).method("getClaim")

val autoClaimRewards = patch("Auto-claim rewards") {
    description("Claims bonus Channel Points in the background as you watch.")
    compatibleWith(TWITCH)
    settings(twitchSettings, section(TwitchStrings.sectionChat, ChatSettings.autoClaimRewards))

    execute {
        gate(ChatSettings.autoClaimRewards) {
            pointsUpdate.before {
                whenInstanceOf(param(0), COMMUNITY_POINTS_MODEL) {
                    val claim = param(0).cast(COMMUNITY_POINTS_MODEL).call(activeClaim)
                    whenNotNull(claim) {
                        val claimId = claim.fieldOfType(Type.String)
                        whenTrue(AutoClaim.isFirstTry(claimId)) {
                            thisObject.fieldOfType(claimBonus.owner).call(claimBonus, claimId, nullObject)
                        }
                    }
                }
            }
        }
    }
}
