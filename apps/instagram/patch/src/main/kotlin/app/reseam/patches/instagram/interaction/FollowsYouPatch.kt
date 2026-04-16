// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later


package app.reseam.patches.instagram.interaction

import app.reseam.patches.instagram.core.FollowSettings
import app.reseam.patches.instagram.core.signatureCheckPatch
import app.reseam.patches.instagram.core.settingsPatch

import app.reseam.patch.Instruction
import app.reseam.patch.Opcodes
import app.reseam.patch.compatibleWith
import app.reseam.patch.fingerprint
import app.reseam.patch.patch
import app.reseam.patch.settings.SettingsSection

private const val EXT = "Lapp/reseam/instagram/follows/FollowsYouIndicator;"

private const val F4C = "LX/F4c;"
private const val SUBTITLE_PROTO =
    "(Landroid/content/Context;Lcom/instagram/common/session/UserSession;LX/E8b;LX/D3X;LX/WZN;)Ljava/lang/String;"

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

        val callSites = insns.withIndex().filter { (_, ins) ->
            ins is Instruction.Invoke &&
                ins.value0.opcode.toInt() == Opcodes.INVOKE_STATIC &&
                ins.value0.method.definingClass == F4C &&
                ins.value0.method.name == "A01" &&
                ins.value0.method.proto == SUBTITLE_PROTO
        }.map { it.index }

        if (callSites.isEmpty()) {
            ctx.log.warn("FollowsYouPatch: no subtitle call sites found in ${method.info.classDescriptor}->${method.info.methodName}")
            return@execute
        }

        var injected = 0
        for (idx in callSites.reversed()) {
            val invoke = insns[idx] as Instruction.Invoke
            val callRegs = invoke.value0.registers
            if (callRegs.size < 5) continue

            val userSessionReg = callRegs[1].toInt()
            val d3xReg = callRegs[3].toInt()
            val subtitleReg = method.registerA(idx + 1)

            val free = method.findFreeRegisters(
                idx + 2,
                count = 2,
                exclude = listOf(subtitleReg, userSessionReg, d3xReg),
            )
            val tmpObj = free[0]
            val tmpStatus = free[1]

            val doneLabel = "fy_done_$idx"

            method.addInstructions(idx + 2) {
                invokeStatic(
                    "LX/D3X;", "A00",
                    "(Lcom/instagram/common/session/UserSession;LX/D3X;)LX/2ai;",
                    userSessionReg, d3xReg,
                )
                moveResultObject(tmpObj)
                ifEqz(tmpObj, doneLabel)
                invokeStatic(
                    "LX/135;", "A0l",
                    "(LX/2ai;)Lcom/instagram/user/model/FriendshipStatus;",
                    tmpObj,
                )
                moveResultObject(tmpStatus)
                ifEqz(tmpStatus, doneLabel)
                invokeInterface(
                    "Lcom/instagram/user/model/FriendshipStatus;", "Bhk",
                    "()Ljava/lang/Boolean;",
                    tmpStatus,
                )
                moveResultObject(tmpStatus)
                invokeStatic(
                    EXT, "maybeAppend",
                    "(Ljava/lang/String;Ljava/lang/Boolean;)Ljava/lang/String;",
                    subtitleReg, tmpStatus,
                )
                moveResultObject(subtitleReg)
                label(doneLabel)
            }
            injected++
        }

        ctx.log.info("FollowsYouPatch: injected at $injected subtitle call sites")
    }
}
