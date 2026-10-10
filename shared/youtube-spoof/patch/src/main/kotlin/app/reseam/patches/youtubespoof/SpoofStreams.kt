// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtubespoof

import app.reseam.patch.ExtClass
import app.reseam.patch.MethodTarget
import app.reseam.patch.PatchRuntime
import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.before
import app.reseam.patch.classTarget
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.Opcode
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.point
import app.reseam.patches.youtubecommon.videoIdGetter

internal const val STREAMING_DATA = "com.google.protos.youtube.api.innertube.StreamingDataOuterClass\$StreamingData"
internal const val BYTE_BUFFER = "java.nio.ByteBuffer"
private const val CRONET_ENGINE = "org.chromium.net.CronetEngine"
private const val SPOOFED_STREAMS = "app.reseam.youtube.spoof.StreamingData"

// Served to the extension's web runtime from assets/reseam/web.
private val WEB_ASSETS = listOf(
    "index.html", "host.js", "page.js", "botguard.js", "player.js",
    "ejs/yt.solver.lib.min.js", "ejs/yt.solver.core.min.js",
)

private object WebAssets

object SpoofVideoStreams : ExtClass("app.reseam.youtube.spoof.SpoofVideoStreams") {
    val setClientOrderToUse by static()
    val isSpoofingEnabled by static(returns = Type.Boolean)
    val rewriteClientContextOsName by static(Type.String, returns = Type.String)
    val fetchStreams by static(Type.String, Type.Map, "[B")
    val getStreamingData by static(Type.String, returns = SPOOFED_STREAMS)
    val getStreamingDataBytes by static(SPOOFED_STREAMS, returns = "[B")
    val onStreamingDataInstalled by static(Type.String, SPOOFED_STREAMS)
    val onNativeStreamingDataInstalled by static(Type.String)
    val onVideoChanged by static(Type.String)
    val rewriteVideoPlaybackMethod by static("android.net.Uri", Type.Int, returns = Type.Int)
    val removeVideoPlaybackPostBody by static(
        "android.net.Uri",
        Type.Int,
        "[B",
        returns = "[B",
    )
    val appendSpoofedClient by static(Type.String, returns = Type.String)
}

/**
 * Fetches each player request's streams from the client chosen in [SpoofSettings] and installs them
 * in place of the app's own, played through the app's native SABR engine. [activityOnCreate] applies
 * the client settings when the app starts.
 */
fun PatchRuntime.spoofStreams(activityOnCreate: MethodTarget) {
    WEB_ASSETS.forEach { name ->
        val asset = WebAssets::class.java.getResourceAsStream("/web/$name")?.use { it.readBytes() }
            ?: error("web/$name is missing from the patch jar")
        files.write("assets/reseam/web/$name", asset)
    }

    hookSabrPlayback()

    activityOnCreate.before { call(SpoofVideoStreams.setClientOrderToUse) }

    cronetUrl.before {
        call(SpoofVideoStreams.fetchStreams, capture("requestUrl"), param(1), paramOfType("[B"))
    }

    createStreamingData.after {
        whenFalse(call(SpoofVideoStreams.isSpoofingEnabled)) { returnVoid() }

        val videoDetails = param(0).field(videoDetailsField)
        whenNull(videoDetails) { returnVoid() }
        val videoId = videoDetails.field(videoIdField)
        whenNull(videoId) { returnVoid() }

        val streams = call(SpoofVideoStreams.getStreamingData, videoId)
        whenNull(streams) {
            call(SpoofVideoStreams.onNativeStreamingDataInstalled, videoId)
            returnVoid()
        }
        val bytes = call(SpoofVideoStreams.getStreamingDataBytes, streams)

        val parser = call(
            protobufParseByteBuffer,
            staticField(playerResponseDefaultInstance),
            callStatic(BYTE_BUFFER, "wrap", "([B)Ljava/nio/ByteBuffer;", bytes),
        )
        val playerResponse = parser.cast(playerResponseClass.descriptor)
        val replacement = playerResponse.field(streamInputField)
        whenNull(replacement) {
            call(SpoofVideoStreams.onNativeStreamingDataInstalled, videoId)
            returnVoid()
        }
        thisObject.set(streamResponseField, replacement)
        call(SabrPlayback.installed, replacement, streams)
        call(SpoofVideoStreams.onStreamingDataInstalled, videoId, streams)
    }

    val mediaUriStore = buildMediaDataSource.point("media request URI store") {
        opcode(Opcode.IPUT_OBJECT)
        field { type("android.net.Uri") }
    }
    val mediaMethodStore = mediaUriStore.next {
        opcode(Opcode.IPUT)
        field { type(Type.Int) }
    }
    val mediaPostDataStore = mediaMethodStore.next {
        opcode(Opcode.IPUT_OBJECT)
        field { type("[B") }
    }
    val mediaUriField = mediaUriStore.field("media request URI field")
    val mediaMethodField = mediaMethodStore.field("media request method field")
    val mediaPostDataField = mediaPostDataStore.field("media request POST body field")
    buildMediaDataSource.after {
        val replacement = call(
            SpoofVideoStreams.removeVideoPlaybackPostBody,
            thisObject.field(mediaUriField),
            thisObject.field(mediaMethodField),
            thisObject.field(mediaPostDataField),
        )
        thisObject.set(mediaPostDataField, replacement)
        thisObject.set(
            mediaMethodField,
            call(
                SpoofVideoStreams.rewriteVideoPlaybackMethod,
                thisObject.field(mediaUriField),
                thisObject.field(mediaMethodField),
            ),
        )
    }
}

private val buildRequest = method("Cronet request builder") {
    calls(klass(CRONET_ENGINE).method("newUrlRequestBuilder"))
    param(1, Type.Map)
}

private val createStreamingData = method("player response streaming data wrapper") {
    calls(klass(STREAMING_DATA).method("getDefaultInstance"))
    flags(AccessFlags.CONSTRUCTOR)
    paramCount(1)
}

private val mediaDataSource = klass("media data source") { strings("DataSpec["); hasInstanceField("[B") }
private val buildMediaDataSource = method("video playback media data source") {
    inClass(mediaDataSource)
    flags(AccessFlags.CONSTRUCTOR)
    hasParam("[B")
}

// The generated message base class keeps its static parsers' names.
private val protobufParseByteBuffer = method("protobuf byte-buffer parser") {
    inClass(playerResponseClass, inherited = true)
    name("parseFrom")
    paramCount(2)
    param(1, BYTE_BUFFER)
}

private val cronetUrl = buildRequest.point("Cronet request URL") {
    invokeVirtual { owner(CRONET_ENGINE); name("newUrlRequestBuilder") }
}.captureArgumentAs("requestUrl", 1, Type.String)

private val streamResponseField = createStreamingData.point("streaming data response field") {
    opcode(Opcode.IPUT_OBJECT)
    field { type(STREAMING_DATA) }
}.field("streaming data response field")

private val streamInputField = createStreamingData.point("player response streaming data field") {
    opcode(Opcode.IGET_OBJECT)
    field { type(STREAMING_DATA) }
}.field("player response streaming data field")

private val playerResponseClass = classTarget("player response protobuf class") {
    bytecode.findClass(streamInputField.owner) ?: error("player response protobuf class is missing")
}

// Follow the app's video-ID getter instead of guessing which protobuf string is the ID.
private val playerResponseModel = klass("player response model") {
    hasInstanceField(playerResponseClass.descriptor)
    implements(videoIdGetter.owner)
}
private val responseVideoId = method("player response video ID") {
    inClass(playerResponseModel)
    name(videoIdGetter.name)
    params()
    returns(Type.String)
}
private val videoDetailsRead = responseVideoId.point("video details read") {
    opcode(Opcode.IGET_OBJECT)
    field { owner(playerResponseClass.descriptor) }
}
private val videoDetailsField = videoDetailsRead.field("video details field")
private val videoIdField = videoDetailsRead.next {
    opcode(Opcode.IGET_OBJECT)
    field { owner(videoDetailsField.type); type(Type.String) }
}.field("video ID field")

private val playerResponseDefaultInstance = playerResponseClass
    .method("<clinit>", inherited = false) {
        params()
        flags(AccessFlags.STATIC)
    }
    .point("player response default instance") {
        opcode(Opcode.SPUT_OBJECT)
        field { type(playerResponseClass.descriptor) }
    }
    .field("player response default instance field")
