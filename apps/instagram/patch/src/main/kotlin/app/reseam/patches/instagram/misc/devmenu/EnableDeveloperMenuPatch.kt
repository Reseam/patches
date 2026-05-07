// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.instagram.misc.devmenu

import app.reseam.patches.instagram.core.DeveloperSettings
import app.reseam.patches.instagram.core.settingsPatch
import app.reseam.patches.instagram.core.signatureCheckPatch

import app.reseam.patch.Opcodes
import app.reseam.patch.classHandle
import app.reseam.patch.compatibleWith
import app.reseam.patch.findMethod
import app.reseam.patch.indexOfFirstInstructionReversed
import app.reseam.patch.methodRef
import app.reseam.patch.opcode
import app.reseam.patch.parameterTypes
import app.reseam.patch.patch
import app.reseam.patch.returnType
import app.reseam.patch.settings.SettingsSection
import app.reseam.patch.settings.returnTrueWhen

private const val USER_SESSION = "Lcom/instagram/common/session/UserSession;"
private const val CLEAR_NOTIFICATION_RECEIVER =
    "Lcom/instagram/notifications/push/ClearNotificationReceiver;"

val enableDeveloperMenuPatch = patch(
    name = "Enable developer menu",
    description = """
        Surfaces Instagram's hidden developer menu as 'Internal Settings' at the bottom
        of the settings screen. Recommended on alpha/beta builds — on stable builds the
        developer flags appear as numeric IDs without descriptions.
    """.trimIndent(),
    compatibleWith = listOf(compatibleWith("com.instagram.android")),
    enabledByDefault = false,
    settingsHost = settingsPatch,
    dependsOn = listOf(signatureCheckPatch, settingsPatch),
    settings = listOf(
        SettingsSection(
            title = "Developer",
            settings = listOf(DeveloperSettings.UnlockDeveloperOptions),
        ),
    ),
) {
    execute { ctx ->
        // ClearNotificationReceiver is registered in AndroidManifest.xml so its class
        // descriptor and onReceive method are stable across versions. We use it as an
        // unobfuscated anchor and walk back from the "NOTIFICATION_DISMISSED" string
        // load to find the static (UserSession)Z developer-menu gate.
        val receiver = ctx.classHandle(CLEAR_NOTIFICATION_RECEIVER, debug = "clearNotificationReceiver")
        val onReceive = ctx.findMethod(debug = "onReceive") {
            inClass(receiver)
            strings("NOTIFICATION_DISMISSED")
        }

        val method = onReceive.method
        val stringIndex = method.indexOfFirstString("NOTIFICATION_DISMISSED")
            ?: error(
                "'NOTIFICATION_DISMISSED' literal missing from " +
                    "${onReceive.classDescriptor}->${onReceive.methodName}",
            )

        // The gate is the most-recent invoke-static{,/range} above the anchor whose
        // callee is `static (UserSession)Z`. That signature is unique on this path,
        // which is why this anchor + signature combo is more robust than scanning
        // the developer-options compose function (whose anchor strings carry
        // version-dependent line numbers).
        val gateCallIndex = method.indexOfFirstInstructionReversed(stringIndex) {
            val ref = methodRef() ?: return@indexOfFirstInstructionReversed false
            (opcode() == Opcodes.INVOKE_STATIC || opcode() == Opcodes.INVOKE_STATIC_RANGE) &&
                ref.parameterTypes == listOf(USER_SESSION) &&
                ref.returnType == "Z"
        }
        require(gateCallIndex >= 0) {
            "Developer-menu gate call (static ($USER_SESSION)Z) not found before " +
                "'NOTIFICATION_DISMISSED' in ${onReceive.classDescriptor}->${onReceive.methodName}"
        }

        val gateRef = method.methodRef(gateCallIndex)
            ?: error("Instruction $gateCallIndex carries no method reference")
        val gateClass = ctx.bytecode.findClass(gateRef.definingClass)
            ?: error("Gate class ${gateRef.definingClass} not present in dex")
        val gateMethod = gateClass.methods.firstOrNull { method ->
            method.info.methodName == gateRef.name && method.info.proto == gateRef.proto
        } ?: error(
            "Gate method ${gateRef.definingClass}->${gateRef.name}${gateRef.proto} " +
                "not present in dex",
        )

        gateMethod.returnTrueWhen(DeveloperSettings.UnlockDeveloperOptions)
        ctx.log.info(
            "Installed developer-menu gate on " +
                "${gateRef.definingClass}->${gateRef.name}${gateRef.proto}",
        )
    }
}
