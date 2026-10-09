// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.misc

import app.reseam.patch.ExtClass
import app.reseam.patch.PatchRuntime
import app.reseam.patch.Type
import app.reseam.patch.before
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.Opcode
import app.reseam.patch.fieldOfType
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.replace
import app.reseam.patches.youtube.internal.appHelper

private const val MEDIA = "com.google.android.libraries.youtube.media.interfaces."
private const val STREAMS = "com.google.protos.youtube.api.innertube.StreamingDataOuterClass\$StreamingData"
private const val COMMON = "com.google.protos.youtube.api.innertube.MediaCommonConfigOuterClass\$MediaCommonConfig"
private const val LIVE = "com.google.protos.youtube.api.innertube.LivePlayerConfigOuterClass\$LivePlayerConfig"
private const val MANIFESTLESS = "com.google.protos.youtube.api.innertube.ManifestlessWindowedLiveConfigOuterClass\$ManifestlessWindowedLiveConfig"
private const val CONFIG = MEDIA + "MediaFetchPlayerConfig"
private const val FETCH = MEDIA + "NetFetch"
private const val CALLBACKS = MEDIA + "NetFetchCallbacks"
private const val REQUEST = MEDIA + "HttpRequest"
private const val TOKEN_MANAGER = MEDIA + "ProofOfOriginTokenManager"
private const val TOKEN_CALLBACK = MEDIA + "OnPoTokenMintedCallback"
private const val BUFFER = "java.nio.ByteBuffer"
private const val REGISTRY = "com.google.protobuf.ExtensionRegistryLite"
private const val ARRAY_LIST = "java.util.ArrayList"

internal object SabrPlayback : ExtClass("app.reseam.youtube.spoof.SabrPlayback") {
    val installed by static(Type.Object, "app.reseam.youtube.spoof.StreamingData")
    val prepare by static(Type.Object, Type.Object, Type.Object, Type.Object, returns = Type.Object)
    val token by static(Type.Object, "[B", returns = "[B")
    val refresh by static(Type.Object, Type.Object, returns = Type.Boolean)
    val request by static(Type.Object, Type.Object, "[B", returns = "[B")
    val response by static(Type.Object, BUFFER)
    val completed by static(Type.Object)
    val replaceConfig by static(Type.Object, Type.Object, returns = Type.Object)
    val parseCommonConfig by static("[B", returns = Type.Object)
    val tokenMinted by static(Type.Object, "[B")
}

/** Uses retained JNI boundaries; all obfuscated members are resolved by their unique type shapes. */
internal fun PatchRuntime.hookSabrPlayback() {
    val configClass = klass(CONFIG)
    val withCommon = appHelper(configClass.classDef, "reseamWithMediaCommonConfig", "(L$COMMON;)L$CONFIG;".replace('.', '/'))
    val live = configClass.fieldOfType(LIVE)
    val manifestless = configClass.fieldOfType(MANIFESTLESS)
    val mode = configClass.fieldOfType(Type.Int)
    val formats = configClass.fieldOfType(ARRAY_LIST)
    withCommon.replace {
        returnValue(
            newInstance(
                CONFIG,
                "(L$LIVE;L$COMMON;L$MANIFESTLESS;ILjava/util/ArrayList;)V".replace('.', '/'),
                thisObject.field(live), param(0), thisObject.field(manifestless),
                thisObject.field(mode), thisObject.field(formats),
            ),
        )
    }
    SabrPlayback.parseCommonConfig.implement {
        val registry = callStatic(REGISTRY, "getGeneratedRegistry", "()Lcom/google/protobuf/ExtensionRegistryLite;")
        val buffer = callStatic(BUFFER, "wrap", "([B)Ljava/nio/ByteBuffer;", param(0))
        val common = callStatic(
            COMMON,
            "parseFrom",
            "(Ljava/nio/ByteBuffer;Lcom/google/protobuf/ExtensionRegistryLite;)L$COMMON;".replace('.', '/'),
            buffer,
            registry,
        )
        returnValue(common)
    }
    SabrPlayback.replaceConfig.implement {
        returnValue(param(0).cast(CONFIG).call(withCommon, param(1).cast(COMMON)))
    }

    SabrPlayback.tokenMinted.implement {
        param(0).cast(TOKEN_CALLBACK).call(tokenMintedCallback, param(1))
        returnVoid()
    }

    createNativePlayback.before {
        param(1).assign(call(SabrPlayback.prepare, param(2), param(1), param(7), param(10)).cast(CONFIG))
        param(9).assign(call(SabrPlayback.token, param(7), param(9)))
    }

    val http = klass(REQUEST)
    val url = http.fieldOfType(Type.String)
    val headers = http.fieldOfType(ARRAY_LIST)
    val body = http.fieldOfType("[B")
    val httpMethod = http.fieldOfType(MEDIA + "HttpMethod")
    val flag = http.fieldOfType(Type.Boolean)
    dispatchRequest.before {
        val original = param(0)
        val bytes = original.field(body)
        val replacement = call(SabrPlayback.request, thisObject, param(1), bytes)
        whenNotEqual(bytes, replacement) {
            param(0).assign(
                newInstance(
                    REQUEST,
                    "(Ljava/lang/String;Ljava/util/ArrayList;[BLcom/google/android/libraries/youtube/media/interfaces/HttpMethod;Z)V",
                    original.field(url), original.field(headers), replacement, original.field(httpMethod), original.field(flag),
                ),
            )
        }
    }
    responseChunk.before { call(SabrPlayback.response, thisObject, param(0)) }
    responseComplete.before { call(SabrPlayback.completed, thisObject) }
    refreshToken.before {
        whenTrue(call(SabrPlayback.refresh, thisObject, param(0))) { returnVoid() }
    }
    currentToken.before {
        val token = call(SabrPlayback.token, thisObject, nullObject)
        whenNotNull(token) { returnValue(token) }
    }
}

private val tokenMintedCallback = method("tokenMintedCallback") {
    inClass(klass(TOKEN_CALLBACK))
    flags(AccessFlags.ABSTRACT)
    params("[B")
    returns(Type.Void)
}

private val createNativePlayback = method("createNativePlayback") {
    inClass(klass(MEDIA + "MediaFetchController\$CppProxy"))
    flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
    param(1, CONFIG)
    param(2, STREAMS)
    param(7, FETCH)
    param(10, TOKEN_MANAGER)
    paramCount(20)
    returns(MEDIA + "PlaybackControllerOrError")
}

// JNI calls these concrete forwarding methods on the retained abstract interface. Hooking here
// reaches every implementation without depending on the obfuscated transport class or method name.
private val dispatchRequest = method("dispatchRequest") {
    inClass(klass(FETCH))
    params(REQUEST, CALLBACKS)
    returns(MEDIA + "NetFetchTask")
    opcode(Opcode.INVOKE_VIRTUAL)
}
private val responseChunk = method("responseChunk") {
    inClass(klass(CALLBACKS + "\$CppProxy"))
    params(BUFFER)
    returns(Type.Void)
    flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
}
private val responseComplete = method("responseComplete") {
    inClass(klass(CALLBACKS + "\$CppProxy"))
    params(MEDIA + "QoeError", Type.Boolean)
    returns(Type.Void)
    flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
}
private val refreshToken = method("refreshToken") {
    inClass(klass(TOKEN_MANAGER))
    params(TOKEN_CALLBACK)
    returns(Type.Void)
    opcode(Opcode.INVOKE_VIRTUAL)
}
private val currentToken = method("currentToken") {
    inClass(klass(TOKEN_MANAGER))
    params()
    returns("[B")
    opcode(Opcode.INVOKE_VIRTUAL)
}
