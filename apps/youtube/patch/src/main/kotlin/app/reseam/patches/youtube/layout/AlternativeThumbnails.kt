// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.layout

import app.reseam.patch.*
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.Opcode
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.*
import app.reseam.patches.youtube.internal.*

val alternativeThumbnails = patch("Alternative thumbnails") {
    description("Replaces thumbnails with DeArrow images or still captures, with separate choices for each feed.")
    compatibleWith(YOUTUBE)
    dependsOn(navigationBarHook, playerTypeHook)
    settings(youTubeSettings, section(YouTubeSettingsPages.Appearance, "Alternative thumbnails",
        YouTubeSettings.altThumbnailHome, YouTubeSettings.altThumbnailSubscriptions,
        YouTubeSettings.altThumbnailLibrary, YouTubeSettings.altThumbnailPlayer,
        YouTubeSettings.altThumbnailSearch, YouTubeSettings.altThumbnailDearrowApiUrl,
        YouTubeSettings.altThumbnailDearrowConnectionToast, YouTubeSettings.altThumbnailStillsFast,
        YouTubeSettings.altThumbnailStillsTime))

    execute {
        imageRequestConstructor.before {
            param(0).assign(call(AlternativeThumbnails.override, param(0)))
        }

        // Pin the image callback by its HTTP parser, then use the stable Cronet interface names.
        val response = klass("org.chromium.net.UrlResponseInfo")
        val getUrl = response.method("getUrl") { params(); returns(Type.String) }
        val getStatus = response.method("getHttpStatusCode") { params(); returns(Type.Int) }
        val callbackOwners = imageResponseStarted.all.map { it.owner }.distinct()
        check(callbackOwners.isNotEmpty()) { "No image response callbacks found" }
        callbackOwners.forEach { owner ->
            val callbacks = klass(owner)
            callbacks.method("onSucceeded") {
                params("org.chromium.net.UrlRequest", response.descriptor)
                returns(Type.Void)
            }.before {
                call(AlternativeThumbnails.onResponse, param(1).call(getUrl), param(1).call(getStatus))
            }
            callbacks.method("onFailed") {
                params("org.chromium.net.UrlRequest", response.descriptor, "org.chromium.net.CronetException")
                returns(Type.Void)
            }.before { call(AlternativeThumbnails.onFailure, param(0)) }
        }

        // A failed request can have no response. Read the original URL through an app-side
        // interface bridge instead of reflection or depending on an obfuscated field name.
        val request = klass("org.chromium.net.impl.CronetUrlRequest")
        val url = request.method("<init>") { flags(AccessFlags.CONSTRUCTOR) }
            .point("Cronet original URL") { opcode(Opcode.IPUT_OBJECT); field { type(Type.String) } }
            .field()
        request.classDef.addInterface("Lapp/reseam/youtube/thumbnails/AlternativeThumbnailsPatch\$ImageRequest;")
        appHelper(request.classDef, "reseam_imageUrl", "()Ljava/lang/String;").replace {
            returnValue(thisObject.field(url))
        }
    }
}

private val imageResponseStarted = methods("image response parsers") {
    name("onResponseStarted")
    strings("Content-Length", "Content-Type", "identity", "application/x-protobuf")
    params("org.chromium.net.UrlRequest", "org.chromium.net.UrlResponseInfo")
    returns(Type.Void)
}

private object AlternativeThumbnails : ExtClass("app.reseam.youtube.thumbnails.AlternativeThumbnailsPatch") {
    val override = static("overrideImageURL", Type.String, returns = Type.String)
    val onResponse = static("onResponse", Type.String, Type.Int)
    val onFailure = static("onFailure", Type.Object)
}
