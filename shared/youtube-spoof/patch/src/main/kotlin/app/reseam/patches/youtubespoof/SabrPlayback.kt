// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtubespoof

import app.reseam.patch.ExtClass
import app.reseam.patch.MethodTarget
import app.reseam.patch.PatchRuntime
import app.reseam.patch.Type
import app.reseam.patch.before
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.fieldOfType
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.methods
import app.reseam.patch.replace
import app.reseam.patches.youtubecommon.appHelper

private const val MEDIA = "com.google.android.libraries.youtube.media.interfaces."
private const val COMMON = "com.google.protos.youtube.api.innertube.MediaCommonConfigOuterClass\$MediaCommonConfig"
private const val LIVE = "com.google.protos.youtube.api.innertube.LivePlayerConfigOuterClass\$LivePlayerConfig"
private const val MANIFESTLESS = "com.google.protos.youtube.api.innertube.ManifestlessWindowedLiveConfigOuterClass\$ManifestlessWindowedLiveConfig"
private const val CONFIG = MEDIA + "MediaFetchPlayerConfig"
private const val FETCH = MEDIA + "NetFetch"
private const val CALLBACKS = MEDIA + "NetFetchCallbacks"
private const val REQUEST = MEDIA + "HttpRequest"
private const val TOKEN_MANAGER = MEDIA + "ProofOfOriginTokenManager"
private const val TOKEN_CALLBACK = MEDIA + "OnPoTokenMintedCallback"
private const val REGISTRY = "com.google.protobuf.ExtensionRegistryLite"
private const val ARRAY_LIST = "java.util.ArrayList"

internal object SabrPlayback : ExtClass("app.reseam.youtube.spoof.SabrPlayback") {
    val installed by static(Type.Object, "app.reseam.youtube.spoof.StreamingData")
    val prepare by static(Type.Object, Type.Object, Type.Object, Type.Object, returns = Type.Object)
    val token by static(Type.Object, "[B", returns = "[B")
    val refresh by static(Type.Object, Type.Object, returns = Type.Boolean)
    val request by static(Type.Object, Type.Object, "[B", returns = "[B")
    val response by static(Type.Object, BYTE_BUFFER)
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
        val buffer = callStatic(BYTE_BUFFER, "wrap", "([B)Ljava/nio/ByteBuffer;", param(0))
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

    // Helpers inside the request class read its fields, which are package-private in some apps.
    val http = klass(REQUEST)
    val url = http.fieldOfType(Type.String)
    val headers = http.fieldOfType(ARRAY_LIST)
    val body = http.fieldOfType("[B")
    val httpMethod = http.fieldOfType(MEDIA + "HttpMethod")
    val flag = http.fieldOfType(Type.Boolean)
    val requestBody = appHelper(http.classDef, "reseamBody", "()[B")
    requestBody.replace { returnValue(thisObject.field(body)) }
    val withBody = appHelper(http.classDef, "reseamWithBody", "([B)L$REQUEST;".replace('.', '/'))
    withBody.replace {
        returnValue(
            newInstance(
                REQUEST,
                "(Ljava/lang/String;Ljava/util/ArrayList;[BL${MEDIA}HttpMethod;Z)V".replace('.', '/'),
                thisObject.field(url), thisObject.field(headers), param(0),
                thisObject.field(httpMethod), thisObject.field(flag),
            ),
        )
    }
    nativeEntries("dispatchRequest", FETCH, MEDIA + "NetFetchTask", REQUEST, CALLBACKS).forEach {
        it.before {
            val bytes = param(0).call(requestBody)
            val replacement = call(SabrPlayback.request, thisObject, param(1), bytes)
            whenNotEqual(bytes, replacement) { param(0).assign(param(0).call(withBody, replacement)) }
        }
    }
    responseChunk.before { call(SabrPlayback.response, thisObject, param(0)) }
    responseComplete.before { call(SabrPlayback.completed, thisObject) }
    nativeEntries("refreshToken", TOKEN_MANAGER, Type.Void, TOKEN_CALLBACK).forEach {
        it.before { whenTrue(call(SabrPlayback.refresh, thisObject, param(0))) { returnVoid() } }
    }
    nativeEntries("currentToken", TOKEN_MANAGER, "[B").forEach {
        it.before {
            val token = call(SabrPlayback.token, thisObject, nullObject)
            whenNotNull(token) { returnValue(token) }
        }
    }
}

private val tokenMintedCallback = method("tokenMintedCallback") {
    inClass(klass(TOKEN_CALLBACK))
    flags(AccessFlags.ABSTRACT)
    params("[B")
}

// The parameters the hook reads; the native twin of each proxy method takes its handle first.
private val createNativePlayback = method("createNativePlayback") {
    inClass(klass(MEDIA + "MediaFetchController\$CppProxy"))
    param(1, CONFIG)
    param(2, STREAMING_DATA)
    param(7, FETCH)
    param(10, TOKEN_MANAGER)
}
private val responseChunk = method("responseChunk") {
    inClass(klass(CALLBACKS + "\$CppProxy"))
    params(BYTE_BUFFER)
}
private val responseComplete = method("responseComplete") {
    inClass(klass(CALLBACKS + "\$CppProxy"))
    params(MEDIA + "QoeError", Type.Boolean)
}

/**
 * The methods JNI calls on the app's implementation of a retained abstract interface method. An app
 * that obfuscates the interface keeps a concrete forwarding method next to the abstract one, which
 * reaches every implementation. Otherwise JNI calls the abstract method by its retained name, so its
 * overrides share that name; the interface's own `CppProxy` is the opposite direction, Java into native.
 */
private fun nativeEntries(label: String, declaringClass: String, returns: String, vararg params: String): List<MethodTarget> {
    val forwarders = methods("$label forwarder") {
        inClass(klass(declaringClass))
        params(*params)
        returns(returns)
        custom { instructionCount > 0 }
    }.all
    if (forwarders.isNotEmpty()) return forwarders

    val declaration = method("$label declaration") {
        inClass(klass(declaringClass))
        flags(AccessFlags.ABSTRACT)
        params(*params)
        returns(returns)
    }
    val overrides = methods(label) {
        name(declaration.name)
        params(*params)
        returns(returns)
        custom { instructionCount > 0 && !owner.endsWith("\$CppProxy;") }
    }.all
    check(overrides.isNotEmpty()) { "No implementation of $declaringClass.${declaration.name}" }
    return overrides
}
