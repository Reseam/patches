// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.telegram.download

import app.reseam.patch.Type
import app.reseam.patch.classTarget
import app.reseam.patch.field
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.replace
import app.reseam.patch.settings.section
import app.reseam.patch.settings.whenEnabled
import app.reseam.patches.telegram.core.TELEGRAM
import app.reseam.patches.telegram.core.TelegramSettings
import app.reseam.patches.telegram.core.telegramSettings

val downloadBoost = patch("Download speed boost") {
    description("Larger chunks (512 KiB) and 8 parallel requests instead of 128 KiB / 4.")
    compatibleWith(TELEGRAM)
    settings(telegramSettings, section("Downloads", TelegramSettings.boostDownloads))

    execute {
        updateParams.replace {
            whenEnabled(TelegramSettings.boostDownloads) {
                thisObject.set(downloadChunkSizeBig, int(512 * 1024))
                thisObject.set(maxDownloadRequests, int(8))
                thisObject.set(maxDownloadRequestsBig, int(8))
                thisObject.set(maxCdnParts, int(4_000))
            } otherwise {
                thisObject.set(downloadChunkSizeBig, int(128 * 1024))
                thisObject.set(maxDownloadRequests, int(4))
                thisObject.set(maxDownloadRequestsBig, int(4))
                thisObject.set(maxCdnParts, int(16_000))
            }
            returnVoid()
        }
    }
}

// FileLoader.DEFAULT_MAX_FILE_SIZE = 2_097_152_000 is baked into the load operation's
// updateParams, which sizes chunks and request parallelism from it.
val updateParams = method("updateParams") {
    name("updateParams")
    literals(2_097_152_000L)
    returns(Type.Void)
    params()
}

val fileLoadOperation = classTarget("fileLoadOperation") { updateParams.method.classDef }
val downloadChunkSizeBig = fileLoadOperation.field("downloadChunkSizeBig")
val maxDownloadRequests = fileLoadOperation.field("maxDownloadRequests")
val maxDownloadRequestsBig = fileLoadOperation.field("maxDownloadRequestsBig")
val maxCdnParts = fileLoadOperation.field("maxCdnParts")
