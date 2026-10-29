// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.gmscore

import app.reseam.gmscore.bindings.GmsCoreSupport
import app.reseam.patch.PatchRuntime
import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.alwaysReturn
import app.reseam.patch.before
import app.reseam.patch.method
import app.reseam.patch.methods
import app.reseam.patch.point
import app.reseam.patch.points

/**
 * Fits the libraries only Google's own apps ship (Primes, notification registration, Cast and the
 * Gservices cache) to GmsCore. [originalPackage] is the package Google's servers know the app by.
 */
fun PatchRuntime.adaptGoogleLibraries(originalPackage: String) {
    // Primes only initializes in an app it recognizes as Google's, judged by the installed
    // package, which neither the rename nor a later Change package name keeps on its list.
    primePackages.alwaysReturn(true)

    // Notification registration is keyed on the package Google's servers know.
    gnpPackageName.after { capture("packageName").assign(string(originalPackage)) }

    castProviderQueries.all.also { check(it.isNotEmpty()) { "No Cast provider discovery queries" } }
        .forEach { query ->
            query.captureArgumentAs("intent", 1, "android.content.Intent").before {
                GmsCoreSupport.restrictCastDiscovery(capture("intent"))
            }
        }

    // GmsCore can serve a Gservices key with no value, and the cache the rows land in rejects
    // nulls, which crashes the app at launch and on every background refresh.
    gservicesCacheFills.forEach {
        captureArgumentAs("rows", 1, Type.Map).before { GmsCoreSupport.dropNullValues(capture("rows")) }
    }
}

// The "prime" method, which matches the running package against the apps Google ships.
private val primePackages = method("primePackages") {
    strings("com.google.android.GoogleCamera")
    returns(Type.Boolean)
}

private val gnpPackageName = method("gnpRegistration") { strings("Exception reading GServices key.") }
    .points("notificationPackageName") {
        invokeVirtual { owner(Type.Context); name("getPackageName"); params(); returns(Type.String) }
    }.single()
    .next { resultOf(Type.String) }
    .captureAs("packageName", Type.String)

// The Gservices prefix query, which copies the rows it read into the app's cache. Older releases
// cache them in a HashMap, which takes nulls.
private val gservicesCacheFills = method("gservicesPrefixQuery") {
    strings("ContentProvider query returned null cursor")
    returns(Type.Void)
    params("android.content.ContentResolver", "java.lang.String[]")
}.points("gservicesCacheFill") { invokeInterface { name("putAll"); params(Type.Map) } }

private val castProviderQueries = methods("castProviderDiscovery") {
    strings("android.media.MediaRouteProviderService")
}.points("castProviderQueries") {
    invokeVirtual {
        owner("android.content.pm.PackageManager")
        name("queryIntentServices")
        params("android.content.Intent", Type.Int)
        returns(Type.List)
    }
}
