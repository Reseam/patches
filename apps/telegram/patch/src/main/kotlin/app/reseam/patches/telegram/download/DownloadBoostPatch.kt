// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.telegram.download

import app.reseam.patch.FieldRef
import app.reseam.patch.buildInstructions
import app.reseam.patch.compatibleWith
import app.reseam.patch.findMethod
import app.reseam.patch.patch
import app.reseam.patch.settings.SettingsSection
import app.reseam.patches.telegram.core.TelegramSettings
import app.reseam.patches.telegram.core.settingsPatch

val downloadBoostPatch = patch(
    name = "Download speed boost",
    description = "Larger chunks (512 KiB) and 8 parallel requests instead of 128 KiB / 4.",
    compatibleWith = listOf(compatibleWith("org.telegram.messenger", "12.7.1")),
    settingsHost = settingsPatch,
    dependsOn = listOf(settingsPatch),
    settings = listOf(
        SettingsSection("Downloads", listOf(TelegramSettings.BoostDownloads)),
    ),
) {
    execute { ctx ->
        // FileLoader.DEFAULT_MAX_FILE_SIZE = 2_097_152_000 is baked into updateParams as a
        // const-wide/32; combined with `()V` + zero params, this uniquely anchors the method.
        val m = ctx.findMethod(debug = "updateParams") {
            literals(2_097_152_000L)
            returnType("V")
            parameterTypes()
        }.method
        val cls = m.info.classDescriptor

        val chunkBig = FieldRef(cls, "downloadChunkSizeBig", "I")
        val maxReq = FieldRef(cls, "maxDownloadRequests", "I")
        val maxReqBig = FieldRef(cls, "maxDownloadRequestsBig", "I")
        val cdnParts = FieldRef(cls, "maxCdnParts", "I")

        // v0/v1 scratch, v2 = `this`. outsSize=2 covers ReseamSettings.getBoolean(String, boolean).
        m.replaceBody(registersSize = 3, outsSize = 2, insns = buildInstructions {
            const4(0, 1)
            constString(1, TelegramSettings.BoostDownloads.key)
            invokeStatic(
                "Lapp/reseam/runtime/settings/ReseamSettings;",
                "getBoolean", "(Ljava/lang/String;Z)Z", 1, 0,
            )
            moveResult(0)
            ifEqz(0, "slow")

            // const-high16 takes the raw upper-16 operand: emit value << 16.
            constHigh16(0, 0x8)  // 0x80000 = 512 KiB
            iput(0, 2, chunkBig)
            const16(0, 8)
            iput(0, 2, maxReq)
            iput(0, 2, maxReqBig)
            const_(0, 4_000)
            iput(0, 2, cdnParts)
            returnVoid()

            label("slow")
            constHigh16(0, 0x2)  // 0x20000 = 128 KiB
            iput(0, 2, chunkBig)
            const4(0, 4)
            iput(0, 2, maxReq)
            iput(0, 2, maxReqBig)
            const_(0, 16_000)
            iput(0, 2, cdnParts)
            returnVoid()
        })
    }
}
