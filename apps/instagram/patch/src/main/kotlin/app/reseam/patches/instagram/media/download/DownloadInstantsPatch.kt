// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.instagram.media.download

import app.reseam.patch.ExtClass
import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.before
import app.reseam.patch.dex.parameterTypes
import app.reseam.patch.dex.returnType
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patches.instagram.core.INSTAGRAM
import app.reseam.patches.instagram.core.USER_SESSION
import app.reseam.patches.instagram.refs.mediaRefs

private const val INSTANTS_VIEWER_PLUGIN = "com.instagram.quicksnap.unified.QuickSnapViewerQPPlugin"
private const val INSTANTS_CAMERA_MODEL = "com.instagram.quicksnap.camera.domain.QuickSnapCameraViewModel"
private const val MEDIA = "com.instagram.feed.media.Media"
private const val USER = "com.instagram.user.model.User"

private object InstantDownloader : ExtClass("app.reseam.instagram.download.InstantDownloader") {
    val attach = static("attach", Type.Object, Type.View)
    val showItem = static("showItem", Type.Object)
    val detach = static("detach", Type.Object)
}

private object InstantMedia : ExtClass("app.reseam.instagram.download.InstantMedia") {
    val media = static("media", Type.Object, returns = Type.Object)
}

private val instantViewer = klass("instantViewer447") {
    hasInstanceField(INSTANTS_VIEWER_PLUGIN)
    hasInstanceField(INSTANTS_CAMERA_MODEL)
}

private val instantItem = klass("instantItem447") {
    hasInstanceField(MEDIA)
    hasInstanceField(USER)
    hasInstanceField(Type.List)
    custom {
        instanceFields.size < 20 &&
            methods.any { it.returnType == "Lcom/instagram/common/typedurl/ImageUrl;" && it.parameterTypes.isEmpty() }
    }
}

private val instantScreen = klass("instantScreen447") {
    hasInstanceField(instantItem.descriptor)
    hasInstanceField(USER_SESSION)
    hasInstanceField("androidx.compose.runtime.MutableState")
    hasInstanceField("kotlin.jvm.functions.Function0")
}

private val viewerCreated = method("instantViewerCreated447") {
    inClass(instantViewer)
    name("onViewCreated")
    returns(Type.Void)
    params(Type.View, "android.os.Bundle")
}

private val viewerDestroyed = method("instantViewerDestroyed447") {
    inClass(instantViewer)
    name("onDestroyView")
    returns(Type.Void)
}

private val renderInstant = method("renderInstant447") {
    inClass(instantScreen)
    name("invoke")
    returns(Type.Object)
    params(Type.Object, Type.Object)
}

val downloadInstants = patch("Download Instants") {
    description("Adds a download button to the Instants viewer.")
    compatibleWith(INSTAGRAM)
    dependsOn(mediaRefs)

    execute {
        viewerCreated.after {
            call(InstantDownloader.attach, thisObject, param(0))
        }
        renderInstant.before {
            val item = thisObject.fieldOfType(instantItem.descriptor)
            call(InstantDownloader.showItem, item)
        }
        viewerDestroyed.before {
            call(InstantDownloader.detach, thisObject)
        }
        InstantMedia.media.implement {
            whenNotNull(param(0)) {
                returnValue(param(0).cast(instantItem.descriptor).fieldOfType(MEDIA))
            }
            returnNull()
        }
    }
}
