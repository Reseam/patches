// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.x.media

import app.reseam.patch.MethodTarget
import app.reseam.patch.Type
import app.reseam.patch.before
import app.reseam.patch.descriptor
import app.reseam.patch.dex.ref
import app.reseam.patch.fieldTarget
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.x.core.X
import app.reseam.patches.x.core.xSettings
import app.reseam.x.download.bindings.XDownloads
import app.reseam.x.settings.XSettings
import app.reseam.x.settings.XStrings

private const val CONTEXT = "Landroid/content/Context;"

val saveMedia = patch("Save media") {
    description("Saves photos, videos and GIFs where the gallery can see them, in a folder you choose.")
    compatibleWith(X)
    settings(xSettings, section(XStrings.sectionMedia, XSettings.downloadFolder))

    execute {
        // Both the save entry and the enqueue carry the user-vs-internal flag as their last argument.
        for (save in listOf(imageAndVideoArgs, enqueueArgs)) {
            save.method.before {
                val handled = XDownloads.save(thisObject.field(downloaderContext), param(save.url), param(save.name), param(save.mime), param(save.headers), param(6))
                whenTrue(handled) { returnVoid() }
            }
        }
    }
}

private class SaveArgs(val method: MethodTarget, val url: Int, val mime: Int, val headers: Int, val name: Int)

private val downloader = klass("downloader") { strings("Starting download: ", "DownloadedFile") }

private val imageAndVideoArgs = SaveArgs(
    method("saveFile") {
        inClass(downloader)
        strings("DownloadedFile")
        returns(Type.Void)
        custom { parameterTypes.size == 7 && parameterTypes[0] == Type.String && parameterTypes[2] == descriptor("java.util.Map") }
    },
    url = 0, mime = 1, headers = 2, name = 3,
)

private val enqueueArgs = SaveArgs(
    method("downloadEnqueue") { inClass(downloader); strings("Starting download: ", ", private=") },
    url = 0, mime = 2, headers = 3, name = 4,
)

private val downloaderContext = fieldTarget("downloaderContext") {
    downloader.classDef.instanceFields.single { it.fieldType == CONTEXT }.ref
}
