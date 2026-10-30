// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.x.media

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
        // The one method every "Save" goes through, before it splits into the image and video
        // paths. Its last argument is true for X's own internal saves, which we leave alone.
        saveFile.before {
            val handled = XDownloads.save(thisObject.field(downloaderContext), param(0), param(3), param(1), param(2), param(6))
            whenTrue(handled) { returnVoid() }
        }
        // The video path also reaches the DownloadManager enqueue directly; cover it too.
        downloadEnqueue.before {
            val handled = XDownloads.save(thisObject.field(downloaderContext), param(0), param(4), param(2), param(3), param(6))
            whenTrue(handled) { returnVoid() }
        }
    }
}

// The file-saving service: holds both the "Save" entry point and the DownloadManager enqueue.
private val downloader = klass("downloader") { strings("Starting download: ", "DownloadedFile") }

// (String url, String mime, Map headers, String name, String subdir, Companion o, boolean internal)
private val saveFile = method("saveFile") {
    inClass(downloader)
    strings("DownloadedFile")
    returns(Type.Void)
    custom { parameterTypes.size == 7 && parameterTypes[0] == Type.String && parameterTypes[2] == descriptor("java.util.Map") }
}

// (Uri url, String title, String mime, Map headers, String name, Companion o, boolean internal)
private val downloadEnqueue = method("downloadEnqueue") { inClass(downloader); strings("Starting download: ", ", private=") }

private val downloaderContext = fieldTarget("downloaderContext") {
    downloader.classDef.instanceFields.single { it.fieldType == CONTEXT }.ref
}
