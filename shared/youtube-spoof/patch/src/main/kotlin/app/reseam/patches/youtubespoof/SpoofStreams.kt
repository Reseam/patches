// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtubespoof

import app.reseam.patch.CodeScope
import app.reseam.patch.MethodTarget
import app.reseam.patch.PatchRuntime
import app.reseam.patch.Type
import app.reseam.patch.ValueRef
import app.reseam.patch.after
import app.reseam.patch.before
import app.reseam.patch.classTarget
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.Opcode
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.point
import app.reseam.patches.youtubecommon.videoIdGetter
import app.reseam.youtube.spoof.bindings.AndroidPoToken
import app.reseam.youtube.spoof.bindings.SabrPlayback
import app.reseam.youtube.spoof.bindings.SpoofVideoStreams
import app.reseam.youtubespoof.settings.SpoofSettings

internal const val STREAMING_DATA = "com.google.protos.youtube.api.innertube.StreamingDataOuterClass\$StreamingData"
internal const val BYTE_BUFFER = "java.nio.ByteBuffer"
private const val CRONET_ENGINE = "org.chromium.net.CronetEngine"
private const val SPOOFED_STREAMS = "app.reseam.youtube.spoof.StreamingData"

// Served to the extension's web runtime from assets/reseam-patches/web.
private val WEB_ASSETS = listOf(
    "index.html", "host.js", "page.js", "botguard.js", "player.js",
    "ejs/yt.solver.lib.min.js", "ejs/yt.solver.core.min.js",
)

private object WebAssets
/**
 * Fetches each player request's streams from the client chosen in [SpoofSettings] and installs them
 * in place of the app's own, played through the app's native SABR engine. [activityOnCreate] applies
 * the client settings when the app starts.
 */
fun PatchRuntime.spoofStreams(activityOnCreate: MethodTarget) {
    WEB_ASSETS.forEach { name ->
        val asset = WebAssets::class.java.getResourceAsStream("/web/$name")?.use { it.readBytes() }
            ?: error("web/$name is missing from the patch jar")
        files.write("assets/reseam-patches/web/$name", asset)
    }

    hookSabrPlayback()
    hookNativePoToken()

    activityOnCreate.before { SpoofVideoStreams.setClientOrderToUse() }

    cronetUrl.before {
        SpoofVideoStreams.fetchStreams(capture("requestUrl"), param(1), paramOfType("[B"))
    }

    createStreamingData.after {
        whenFalse(SpoofVideoStreams.isSpoofingEnabled()) { returnVoid() }
        whenNotNull(param(0).field(offlineStateField)) { returnVoid() }

        val videoDetails = param(0).field(videoDetailsField)
        whenNull(videoDetails) { returnVoid() }
        val videoId = videoDetails.field(videoIdField)
        whenNull(videoId) { returnVoid() }

        val streams = SpoofVideoStreams.getStreamingData(videoId)
        whenNull(streams) {
            SpoofVideoStreams.onNativeStreamingDataInstalled(videoId)
            returnVoid()
        }
        val bytes = SpoofVideoStreams.getStreamingDataBytes(streams)

        val playerResponse = parsePlayerResponse(bytes)
        val replacement = playerResponse.field(streamInputField)
        whenNull(replacement) {
            SpoofVideoStreams.onNativeStreamingDataInstalled(videoId)
            returnVoid()
        }
        thisObject.set(streamResponseField, replacement)
        SabrPlayback.installed(replacement, streams)
        SpoofVideoStreams.onStreamingDataInstalled(videoId, streams)
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
        val replacement = SpoofVideoStreams.removeVideoPlaybackPostBody(thisObject.field(mediaUriField), thisObject.field(mediaMethodField), thisObject.field(mediaPostDataField))
        thisObject.set(mediaPostDataField, replacement)
        thisObject.set(
            mediaMethodField,
            SpoofVideoStreams.rewriteVideoPlaybackMethod(thisObject.field(mediaUriField), thisObject.field(mediaMethodField)),
        )
    }
}

/**
 * Supplies the native client's integrity token from the server for a native-mode video, instead of
 * the local Play Services the patched app cannot genuinely run. The extension returns null for every
 * other request, so the app's own path is left untouched.
 */
private fun PatchRuntime.hookNativePoToken() {
    integrityTokenGenerate.before {
        val token = AndroidPoToken.mintNativePoToken(param(0), param(1))
        whenNotNull(token) { returnValue(newInstance(integrityResult.descriptor, "([B)V", token)) }
    }
}

private val integrityTokenManager = klass("integrity token manager") {
    strings(
        "Generating PO Token failed.",
        "Invalid mode",
        "Unable to generate a new integrity token.",
    )
}

// The inner mint call, below the manager's availability precheck and its result caching; replacing
// it leaves the native attestation handshake intact while only the token itself comes from us.
private val integrityTokenGenerate = method("integrity token generate") {
    inClass(integrityTokenManager)
    paramCount(6)
    param(0, "[B")
    param(1, Type.Int)
}

private val integrityResult = classTarget("integrity token result") {
    bytecode.findClass(integrityTokenGenerate.returnType) ?: error("The integrity token result is missing")
}

private val buildRequest = method("Cronet request builder") {
    calls(klass(CRONET_ENGINE).method("newUrlRequestBuilder"))
    param(1, Type.Map)
}

internal val createStreamingData = method("player response streaming data wrapper") {
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
internal fun CodeScope.parsePlayerResponse(bytes: ValueRef) = call(
    protobufParseByteBuffer,
    staticField(playerResponseDefaultInstance),
    callStatic(BYTE_BUFFER, "wrap", "([B)Ljava/nio/ByteBuffer;", bytes),
).cast(playerResponseClass.descriptor)

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

internal val streamInputField = createStreamingData.point("player response streaming data field") {
    opcode(Opcode.IGET_OBJECT)
    field { type(STREAMING_DATA) }
}.field("player response streaming data field")

internal val playerResponseClass = classTarget("player response protobuf class") {
    bytecode.findClass(streamInputField.owner) ?: error("player response protobuf class is missing")
}

// Follow the app's video-ID getter instead of guessing which protobuf string is the ID.
internal val playerResponseModel = klass("player response model") {
    hasInstanceField(playerResponseClass.descriptor)
    implements(videoIdGetter.owner)
}

internal val responseVideoId = method("player response video ID") {
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
