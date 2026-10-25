// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtubesavetodevice

import app.reseam.patch.PatchRuntime
import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.before
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.point
import app.reseam.patch.reserveLocal
import app.reseam.youtube.savetodevice.bindings.SaveToDevice

fun PatchRuntime.saveNewDownloads() {
    val videoId = mediaStatusUpdate.reserveLocal("savedVideoId", Type.String)
    val status = mediaStatusUpdate.reserveLocal("savedStatus", Type.Object)
    mediaStatusUpdate.before {
        local(videoId).assign(param(0))
        local(status).assign(param(1))
    }
    // The store returns early when the status is unchanged, so this write is a real transition.
    mediaStatusUpdate.point { invokeVirtual { params(Type.String, mediaStatus.descriptor); returns(Type.Void) } }.after {
        SaveToDevice.onDownloadStatus(
            local(videoId),
            local(status).cast("java.lang.Enum").callVirtual("java.lang.Enum", "name", "()Ljava/lang/String;"),
        )
    }
}

private val mediaStatus = klass("offline media status") {
    strings("STREAM_DOWNLOAD_PENDING", "REQUIRES_CONTENT_VERIFICATION")
}

// The offline store's locked setter, which hands a changed status on to the stream store's writer.
private val mediaStatusUpdate = method("offline media status update") {
    params(Type.String, mediaStatus.descriptor)
    flags(AccessFlags.DECLARED_SYNCHRONIZED)
    returns(Type.Void)
    calls { params(Type.String, mediaStatus.descriptor); returns(Type.Void) }
}
