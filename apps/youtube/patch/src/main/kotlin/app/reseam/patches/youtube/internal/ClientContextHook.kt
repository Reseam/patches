// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.internal

import app.reseam.patch.CodeScope
import app.reseam.patch.ValueRef
import app.reseam.patch.ExtMethod
import app.reseam.patch.MethodTarget
import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.classTarget
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.Opcode
import app.reseam.patch.dex.returnType
import app.reseam.patch.field
import app.reseam.patch.fieldTarget
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.replace
import app.reseam.patch.settings.ToggleSetting
import app.reseam.patch.settings.whenEnabled
import app.reseam.patches.youtube.core.YOUTUBE

private const val CLIENT_INFO = "com.google.protos.youtube.api.innertube.InnertubeContext\$ClientInfo"
private const val BUILD_VERSION = "android.os.Build\$VERSION"
private const val INNER_CONTEXT = "com.google.protos.youtube.api.innertube.InnertubeContext\$InnerTubeContext"
private const val HELPER = "patch_setClientContext"

/** The request families whose client context a patch can rewrite. */
enum class ClientContextEndpoint { BROWSE, SEARCH, REEL, GUIDE }

private val clientInfoHooks = linkedMapOf<ClientContextEndpoint, MutableList<CodeScope.(ValueRef) -> Unit>>()

/**
 * Passes the OS name the app reports on [endpoint] requests through [hook], a static
 * `(String) -> String`. Call from `execute` of a patch that depends on [clientContextHook]; the
 * hooks are emitted once every dependent has registered.
 */
fun hookClientContextOsName(endpoint: ClientContextEndpoint, hook: ExtMethod) {
    clientInfoHooks.getOrPut(endpoint) { mutableListOf() } += { clientInfo ->
        clientInfo.set(osNameField, call(hook, clientInfo.field(osNameField)))
    }
}

/** A settings-gated constant override, emitted directly into the endpoint helper. */
fun overrideClientContextOsName(endpoint: ClientContextEndpoint, setting: ToggleSetting, value: String) {
    clientInfoHooks.getOrPut(endpoint) { mutableListOf() } += { clientInfo ->
        whenEnabled(setting) { clientInfo.set(osNameField, string(value)) }
    }
}

/**
 * Gives each endpoint class a `patch_setClientContext()` that rewrites the OS name on the request's
 * client info, and calls it where the endpoint finishes building its body. The app has no single
 * place that sets the OS name per request, so the helper is synthesised per endpoint class.
 */
val clientContextHook = patch {
    compatibleWith(YOUTUBE)

    afterDependents {
        if (clientInfoHooks.isEmpty()) return@afterDependents

        val registeredHooks = clientInfoHooks.mapValues { it.value.toList() }
        clientInfoHooks.clear()
        for ((endpoint, hooks) in registeredHooks) {
            val sources = endpointSources(endpoint).distinctBy { it.owner }
            check(sources.isNotEmpty()) { "No $endpoint request constructors found" }
            log.debug("$endpoint client contexts: ${sources.size} endpoint classes")
            for (source in sources) {
                val endpointClass = classTarget("$endpoint endpoint class") {
                    bytecode.findClass(source.owner) ?: error("${source.owner} is missing")
                }
                // The endpoint's protected final no-arg method is where its request body is complete.
                val endpointBody = method("$endpoint endpoint body") {
                    inClass(endpointClass)
                    flags(AccessFlags.PROTECTED or AccessFlags.FINAL)
                    params()
                    returns(Type.Void)
                }
                val contextBuilder = method("$endpoint client-context builder") {
                    inClass(endpointClass, inherited = true)
                    params()
                    returns(builderField.owner)
                }
                val helper = appHelper(endpointClass.classDef, HELPER, "()V",
                    AccessFlags.PRIVATE or AccessFlags.FINAL)
                helper.replace {
                    val clientInfo = thisObject
                        .call(contextBuilder)
                        .field(builderField)
                        .cast(clientInfoField.owner)
                        .field(clientInfoField)
                    whenNotNull(clientInfo) {
                        for (hook in hooks) hook(clientInfo)
                    }
                    returnVoid()
                }
                endpointBody.after { thisObject.call(helper) }
            }
        }
    }
}

// The one constructor that picks the OS name by form factor.
val clientContextConstructor = method("clientContextConstructor") {
    strings("Android Automotive")
    flags(AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR)
    returns(Type.Void)
}

val clientContextClass = classTarget("clientContextClass") {
    bytecode.findClass(clientContextConstructor.owner) ?: error("The client context class is missing")
}

val clientInfoBuilder = klass(CLIENT_INFO).method("createBuilder", inherited = true) { params() }

// The builder return type separates this from the method that returns a finished ClientInfo.
val clientContextBody = method("clientContextBody") {
    inClass(clientContextClass, inherited = true)
    params()
    returns(clientInfoBuilder.returnType)
}

// The String the body stores right after reading the SDK level is the OS name.
val osNameField = clientContextBody
    .point("sdkInt") { field { owner(BUILD_VERSION); name("SDK_INT") } }
    .next { opcode(Opcode.IPUT_OBJECT); field { owner(CLIENT_INFO); type(Type.String) } }
    .field()

// Protobuf keeps the builder storage name even when its class is obfuscated.
val builderField = fieldTarget("clientInfoBuilderStorage") {
    klass(clientInfoBuilder.returnType).field("instance").ref
}

// The placeholder context writes the primary ClientInfo, distinguishing it from
// the second ClientInfo field used by newer context messages.
val dummyClientContext = method("dummyClientContext") { strings("10.29") }
val clientInfoField = dummyClientContext.point("primaryClientInfo") {
    opcode(Opcode.IPUT_OBJECT)
    field { owner(INNER_CONTEXT); type(CLIENT_INFO) }
}.field()

val browseEndpointParent = method("browseEndpointParent") {
    strings("browseId")
    params()
    returns(Type.String)
}

val searchEndpointParent = method("searchEndpointParent") {
    strings("searchFormData")
    flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
    params()
    returns(Type.String)
    calls { name("toByteArray"); returns("[B") }
}

// Discover the request family actually shipped by this APK. Constructors distinguish
// endpoint implementations from other references to these routes (such as allowlists).
val reelEndpoints = methods("reelEndpoints") {
    stringsStartingWith("reel/")
    name("<init>")
}

val guideEndpoint = method("guideEndpoint") {
    strings("guide")
    flags(AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR)
    returns(Type.Void)
}

private fun endpointSources(endpoint: ClientContextEndpoint): List<MethodTarget> = when (endpoint) {
    ClientContextEndpoint.BROWSE -> listOf(browseEndpointParent)
    ClientContextEndpoint.SEARCH -> listOf(searchEndpointParent)
    ClientContextEndpoint.REEL -> reelEndpoints.all
    ClientContextEndpoint.GUIDE -> listOf(guideEndpoint)
}
