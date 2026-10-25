// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.internal

import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.classTarget
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtubecommon.parseProtobuf
import app.reseam.youtube.navigation.bindings.Commands

/** Gives the navigation extension's Commands the app's command router. */
val commandRouter = patch {
    compatibleWith(YOUTUBE)

    execute {
        intentResolverConstructor.after { Commands.setRouter(thisObject.fieldOfType(routeEndpoint.owner)) }
        Commands.dispatch.implement {
            val endpoint = parseProtobuf(navigationEndpoint, "navigation endpoint", param(1))
            param(0).cast(routeEndpoint.owner)
                .callInterface(routeEndpoint.owner, routeEndpoint.name, routeEndpoint.proto, endpoint, nullObject)
            returnVoid()
        }
    }
}

// The one class that opens Digital Wellbeing's app usage page.
private val intentResolver = klass("intentResolver") {
    strings("com.google.android.apps.wellbeing.VIEW_APP_USAGE")
}

private val intentResolverConstructor = intentResolver.methods("intentResolverConstructor") {
    flags(AccessFlags.CONSTRUCTOR)
}.single { true }

private val resolveIntent = method("resolveIntent") {
    inClass(intentResolver)
    strings("navigation_endpoint")
    params("android.content.Intent", Type.Boolean)
}

// Pages an intent names are routed as commands, the same way taps inside the app are.
internal val routeEndpoint = resolveIntent.point("routeEndpoint") {
    invokeInterface { paramCount(2); hasParam(Type.Map); returns(Type.Void) }
}.callee("routeEndpoint")

internal val navigationEndpoint = classTarget("navigationEndpoint") {
    bytecode.findClass(routeEndpoint.parameterTypes[0]) ?: error("The navigation endpoint class is missing")
}
