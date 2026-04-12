/*
 * Stitch — part of the Stitch Android patcher.
 * Copyright (C) 2026 Aunali321 <accounts@auna.li>
 *
 * This file is part of Stitch.
 *
 * Stitch is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Stitch is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package dev.stitch.patches.instagram.interaction

import dev.stitch.patches.instagram.core.FollowSettings
import dev.stitch.patches.instagram.core.signatureCheckPatch
import dev.stitch.patches.instagram.core.settingsPatch

import dev.stitch.patch.Opcodes
import dev.stitch.patch.compatibleWith
import dev.stitch.patch.fingerprint
import dev.stitch.patch.methodRef
import dev.stitch.patch.opcode
import dev.stitch.patch.patch
import dev.stitch.patch.returnType
import dev.stitch.patch.stringValue
import dev.stitch.patch.settings.SettingsSection

private val friendshipStatusPropertyMapFingerprint = fingerprint {
    strings("followed_by", "blocking", "following")
    returnType("Ljava/util/Map;")
}

private val searchSubtitleFingerprint = fingerprint {
    strings(" \u2022 ")
    returnType("Ljava/lang/String;")
}

val followsYouPatch = patch(
    name = "Follows you indicator",
    description = "Shows a 'Follows you' badge next to usernames in search results and user lists",
    compatibleWith = listOf(compatibleWith("com.instagram.android")),
    settingsHost = settingsPatch,
    dependsOn = listOf(signatureCheckPatch, settingsPatch),
    enabledByDefault = false,
    settings = listOf(
        SettingsSection(
            title = "Social",
            settings = listOf(FollowSettings.FollowsYouIndicator),
        ),
    ),
) {
    extendWith("follows-you.dex")

    execute { ctx ->
        val propMapMethod = friendshipStatusPropertyMapFingerprint.method
        val insns = propMapMethod.instructions

        var followedByMethodName: String? = null
        var friendshipInterface: String? = null

        for (i in insns.indices) {
            if (insns[i].stringValue() != "followed_by") continue
            for (j in (i + 1)..(i + 3).coerceAtMost(insns.lastIndex)) {
                val ref = insns[j].methodRef() ?: continue
                if (ref.definingClass.contains("FriendshipStatus")) {
                    followedByMethodName = ref.name
                    friendshipInterface = ref.definingClass
                    break
                }
            }
            if (followedByMethodName != null) break
        }

        if (followedByMethodName == null || friendshipInterface == null) {
            ctx.log.warn("Could not resolve followed_by method on FriendshipStatus")
            return@execute
        }

        ctx.log.info("Found followed_by: $friendshipInterface->$followedByMethodName")

        if (!searchSubtitleFingerprint.matched) {
            ctx.log.warn("Search subtitle fingerprint not matched")
            return@execute
        }

        val subtitleMethod = searchSubtitleFingerprint.method
        val subtitleInsns = subtitleMethod.instructions
        val returnIndices = subtitleInsns.mapIndexedNotNull { i, insn ->
            if (insn.opcode() == Opcodes.RETURN_OBJECT) i else null
        }

        var injected = 0
        for (retIdx in returnIndices.reversed()) {
            val retReg = subtitleMethod.registerA(retIdx)
            subtitleMethod.insertInvokeStaticWithMoveResult(
                retIdx,
                "Ldev/stitch/extension/instagram/follows/FollowsYouIndicator;",
                "maybeAppend",
                "(Ljava/lang/String;)Ljava/lang/String;",
                listOf(retReg),
                retReg,
                isObject = true,
            )
            injected++
        }

        ctx.log.info("Injected follows-you check at $injected return points")
    }
}
