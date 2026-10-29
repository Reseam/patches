// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.gmscore

import app.reseam.patch.Type
import app.reseam.patch.method

// GooglePlayServicesUtil.ensurePlayServicesAvailable, which throws when they are not. Newer
// releases take the minimum version as a second parameter.
internal val playServicesCheck = method("playServicesCheck") {
    strings("Google Play Services not available")
    returns(Type.Void)
    param(0, Type.Context)
}

// GooglePlayServicesUtilLight.isGooglePlayServicesAvailable: a ConnectionResult status code where
// zero is SUCCESS, not a boolean.
internal val playServicesAvailability = method("playServicesAvailability") {
    strings("com.google.android.gms.version")
    params(Type.Context, Type.Int)
    returns(Type.Int)
}
