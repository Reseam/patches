// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.gmscore

import app.reseam.patch.Type
import app.reseam.patch.method
import app.reseam.patch.methods
import app.reseam.patch.point
import app.reseam.patch.points

// GooglePlayServicesUtil.ensurePlayServicesAvailable, which throws when they are not.
internal val playServicesCheck = method("playServicesCheck") {
    strings("Google Play Services not available")
    returns(Type.Void)
    params(Type.Context, Type.Int)
}

// GooglePlayServicesUtilLight.isGooglePlayServicesAvailable: a ConnectionResult status code where
// zero is SUCCESS, not a boolean.
internal val playServicesAvailability = method("playServicesAvailability") {
    strings("com.google.android.gms.version")
    params(Type.Context, Type.Int)
    returns(Type.Int)
}

// The "prime" method, which matches the running package against the apps Google ships.
internal val primePackages = method("primePackages") {
    strings("com.google.android.GoogleCamera")
    returns(Type.Boolean)
}

internal val gnpPackageName = method("gnpRegistration") { strings("Exception reading GServices key.") }
    .points("notificationPackageName") {
        invokeVirtual { owner(Type.Context); name("getPackageName"); params(); returns(Type.String) }
    }.single()
    .next { resultOf(Type.String) }
    .captureAs("packageName", Type.String)

// The Gservices prefix query, which copies the rows it read into the app's cache. Older releases
// cache them in a HashMap, which takes nulls.
internal val gservicesCacheFills = method("gservicesPrefixQuery") {
    strings("ContentProvider query returned null cursor")
    returns(Type.Void)
    params("android.content.ContentResolver", "java.lang.String[]")
}.points("gservicesCacheFill") { invokeInterface { name("putAll"); params(Type.Map) } }

internal val castProviderQueries = methods("castProviderDiscovery") {
    strings("android.media.MediaRouteProviderService")
}.points("castProviderQueries") {
    invokeVirtual {
        owner("android.content.pm.PackageManager")
        name("queryIntentServices")
        params("android.content.Intent", Type.Int)
        returns(Type.List)
    }
}
