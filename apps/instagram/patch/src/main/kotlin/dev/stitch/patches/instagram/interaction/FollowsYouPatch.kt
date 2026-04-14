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

import dev.stitch.patch.Instruction
import dev.stitch.patch.MethodRef
import dev.stitch.patch.Opcodes
import dev.stitch.patch.compatibleWith
import dev.stitch.patch.fingerprint
import dev.stitch.patch.parameterTypes
import dev.stitch.patch.patch
import dev.stitch.patch.settings.SettingsSection

private const val EXT = "Ldev/stitch/instagram/follows/FollowsYouIndicator;"
private const val USER_SESSION = "Lcom/instagram/common/session/UserSession;"
private const val FRIENDSHIP_STATUS = "Lcom/instagram/user/model/FriendshipStatus;"

// Row binder in the search-result subtitle builder. Strings are stable across versions.
private val searchRowBinderFingerprint = fingerprint {
    strings("search_navigate_to_user", " \u2022 ")
    returnType("V")
}

val followsYouPatch = patch(
    name = "Follows you indicator",
    description = "Shows a 'Follows you' badge next to usernames in search results",
    compatibleWith = listOf(compatibleWith("com.instagram.android")),
    settingsHost = settingsPatch,
    dependsOn = listOf(signatureCheckPatch, settingsPatch),
    enabledByDefault = true,
    settings = listOf(
        SettingsSection(
            title = "Social",
            settings = listOf(FollowSettings.FollowsYouIndicator),
        ),
    ),
) {
    extendWith("instagram-follows-you.dex")

    execute { ctx ->
        val method = searchRowBinderFingerprint.method
        val insns = method.instructions

        fun staticCalls() = insns.withIndex().mapNotNull { (i, ins) ->
            if (ins is Instruction.Invoke && ins.value0.opcode.toInt() == Opcodes.INVOKE_STATIC)
                i to ins else null
        }
        val statics = staticCalls()

        // Resolve the (userModel -> FriendshipStatus) helper by its stable return type.
        val statusHelper = statics.map { it.second.value0.method }.firstOrNull {
            it.proto.endsWith(")$FRIENDSHIP_STATUS") && it.parameterTypes.size == 1
        } ?: run {
            ctx.log.warn("FollowsYouPatch: status helper not found"); return@execute
        }
        val userModelType = statusHelper.parameterTypes[0]

        // Resolve the (UserSession, D3X -> userModel) helper.
        val userResolver = statics.map { it.second.value0.method }.firstOrNull {
            it.proto.endsWith(")$userModelType") &&
                it.parameterTypes.size == 2 &&
                it.parameterTypes[0] == USER_SESSION
        } ?: run {
            ctx.log.warn("FollowsYouPatch: user resolver not found"); return@execute
        }
        val d3xType = userResolver.parameterTypes[1]

        // Follow-state getter: invoke-interface on FriendshipStatus returning Boolean,
        // whose receiver traces back to the status helper call.
        val followedByName = run {
            for ((i, ins) in insns.withIndex()) {
                if (ins !is Instruction.Invoke) continue
                val m = ins.value0.method
                if (ins.value0.opcode.toInt() != Opcodes.INVOKE_INTERFACE) continue
                if (m.definingClass != FRIENDSHIP_STATUS) continue
                if (!m.proto.endsWith(")Ljava/lang/Boolean;")) continue
                val recv = ins.value0.registers[0].toInt()
                for (j in (i - 1) downTo maxOf(0, i - 6)) {
                    val prev = insns[j] as? Instruction.Invoke ?: continue
                    if (prev.value0.method != statusHelper) continue
                    if (method.registerA(j + 1) == recv) return@run m.name
                }
            }
            null
        } ?: run {
            ctx.log.warn("FollowsYouPatch: followed_by getter not found"); return@execute
        }

        // Subtitle builder: static String-returning call taking (..., UserSession, ..., D3X, ...).
        val subtitleSites = statics.filter { (_, ins) ->
            val m = ins.value0.method
            m.proto.endsWith(")Ljava/lang/String;") &&
                m.parameterTypes.contains(USER_SESSION) &&
                m.parameterTypes.contains(d3xType)
        }
        if (subtitleSites.isEmpty()) {
            ctx.log.warn("FollowsYouPatch: subtitle call sites not found"); return@execute
        }

        var injected = 0
        for ((idx, ins) in subtitleSites.reversed()) {
            val params = ins.value0.method.parameterTypes
            val callRegs = ins.value0.registers
            val userSessionReg = callRegs[params.indexOf(USER_SESSION)].toInt()
            val d3xReg = callRegs[params.indexOf(d3xType)].toInt()
            val subtitleReg = method.registerA(idx + 1)

            val (tmp1, tmp2) = method.findFreeRegisters(
                idx + 2, 2,
                exclude = listOf(subtitleReg, userSessionReg, d3xReg),
            )

            val done = "fy_done_$idx"
            method.addInstructions(idx + 2) {
                invokeStatic(userResolver.definingClass, userResolver.name, userResolver.proto,
                    userSessionReg, d3xReg)
                moveResultObject(tmp1)
                ifEqz(tmp1, done)
                invokeStatic(statusHelper.definingClass, statusHelper.name, statusHelper.proto, tmp1)
                moveResultObject(tmp2)
                ifEqz(tmp2, done)
                invokeInterface(FRIENDSHIP_STATUS, followedByName, "()Ljava/lang/Boolean;", tmp2)
                moveResultObject(tmp2)
                invokeStatic(EXT, "maybeAppend",
                    "(Ljava/lang/String;Ljava/lang/Boolean;)Ljava/lang/String;",
                    subtitleReg, tmp2)
                moveResultObject(subtitleReg)
                label(done)
            }
            injected++
        }

        ctx.log.info("FollowsYouPatch: injected at $injected subtitle sites")
    }
}
