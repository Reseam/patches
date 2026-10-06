// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.gms

import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.method
import app.reseam.patch.point
import app.reseam.patches.gmscore.gmsCoreSupportFor
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.mainActivityOnCreate
import app.reseam.patches.youtube.internal.enableBooleanFeature

// GmsCore has no Phenotype service, so until the app's own config sync takes effect (its third
// launch) every experiment reads its compiled-in default. These flags are on for every client, and
// the release breaks with their default: 45698813 off registers the watch-page timebar under no
// player state, so the player finds no active timebar, draws no seekbar and shows 0:00.
private val PHENOTYPE_DELIVERED_FLAGS = listOf(45698813L)

val gmsCoreSupport = gmsCoreSupportFor(
    YOUTUBE,
    signature = "24bb24c05e47e0aefa68a58a766179d9b613a600",
    defaultPackageName = "app.reseam.android.youtube",
    mainActivityOnCreate,
) {
    // GmsCore reports the device as not Play Protect certified, and the app answers by hiding
    // behind UncertifiedDeviceActivity. With the check's Gservices switch read as off, the app
    // takes its own path that skips the check.
    deviceComplianceCheckEnabled.after { capture("enabled").assign(bool(false)) }
    PHENOTYPE_DELIVERED_FLAGS.forEach(::enableBooleanFeature)
}

private const val DEVICE_COMPLIANCE_CHECK_FLAG = "failsafe_enable_gms_device_compliance_check"

// Asks Play Services whether the device is Play Protect certified and emits the answer.
private val deviceComplianceCheckEnabled = method("deviceComplianceCheck") {
    strings(DEVICE_COMPLIANCE_CHECK_FLAG)
    returns(Type.Void)
}.point("deviceComplianceCheckEnabled") { string(DEVICE_COMPLIANCE_CHECK_FLAG) }
    .next { resultOf(Type.Boolean) }
    .captureAs("enabled", Type.Boolean)
