// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.gms

import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.before
import app.reseam.patch.method
import app.reseam.patch.point
import app.reseam.patches.gmscore.adaptGoogleLibraries
import app.reseam.patches.gmscore.gmsCoreSupportFor
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.mainActivityOnCreate
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.patches.youtube.internal.enableBooleanFeature
import app.reseam.youtube.misc.bindings.OwnLinks

// GmsCore has no Phenotype service, so until the app's own config sync takes effect (its third
// launch) every experiment reads its compiled-in default. These flags are on for every client, and
// the release breaks with their default: 45698813 off registers the watch-page timebar under no
// player state, so the player finds no active timebar, draws no seekbar and shows 0:00. 20.26 predates it.
private val PHENOTYPE_DELIVERED_FLAGS = listOf(45698813L)

val gmsCoreSupport = gmsCoreSupportFor(
    YOUTUBE,
    youTubeSettings,
    signature = "24bb24c05e47e0aefa68a58a766179d9b613a600",
    defaultPackageName = "app.reseam.android.youtube",
    mainActivityOnCreate,
) {
    adaptGoogleLibraries(YOUTUBE.name)
    // GmsCore reports the device as not Play Protect certified, and the app answers by hiding
    // behind UncertifiedDeviceActivity. With the check's Gservices switch read as off, the app
    // takes its own path that skips the check.
    deviceComplianceCheckEnabled.after { capture("enabled").assign(bool(false)) }
    PHENOTYPE_DELIVERED_FLAGS.forEach(::enableBooleanFeature)
    ownLinkIntent.before { OwnLinks.keepInApp(param(0), param(1), param(2)) }
}

// Marks a link YouTube sends to itself with a token proving where it came from.
private val ownLinkIntent = method("ownLinkIntent") {
    strings("trusted_application_code_extra")
    params(Type.Context, "android.content.Intent", "android.net.Uri")
}

private const val DEVICE_COMPLIANCE_CHECK_FLAG = "failsafe_enable_gms_device_compliance_check"

// Asks Play Services whether the device is Play Protect certified and emits the answer.
private val deviceComplianceCheckEnabled = method("deviceComplianceCheck") {
    strings(DEVICE_COMPLIANCE_CHECK_FLAG)
    returns(Type.Void)
}.point("deviceComplianceCheckEnabled") { string(DEVICE_COMPLIANCE_CHECK_FLAG) }
    .next { resultOf(Type.Boolean) }
    .captureAs("enabled", Type.Boolean)
