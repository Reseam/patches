// SPDX-FileCopyrightText: 2026 MadKarma <me@madkarma.top>
// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.universal

import app.reseam.patch.*
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.DexClass

const val PAIRIP_DESCRIPTOR_PREFIX = "Lcom/pairip/"
const val SIGNATURE_CHECK = "com.pairip.SignatureCheck"
const val VM_RUNNER = "com.pairip.VMRunner"
const val STARTUP_LAUNCHER = "com.pairip.StartupLauncher"
const val PAIRIP_APPLICATION = "com.pairip.application.Application"
const val LICENSE_CLIENT = "com.pairip.licensecheck.LicenseClient"
const val LICENSE_CLIENT_V3 = "com.pairip.licensecheck3.LicenseClientV3"
const val LICENSE_CONTENT_PROVIDER = "com.pairip.licensecheck.LicenseContentProvider"
const val LICENSE_ACTIVITY = "com.pairip.licensecheck.LicenseActivity"
const val PLAY_STORE = "com.android.vending"

val LICENSE_CLIENT_VOID_METHODS = arrayOf(
    "checkLicense",
    "initializeLicenseCheck",
    "connectToLicensingService",
    "retryOrThrow",
    "lambda\$retryOrThrow\$0",
    "startPaywallActivity",
    "startErrorDialogActivity",
    "scheduleAppShutdown",
)

val LICENSE_ACTIVITY_VOID_METHODS = arrayOf(
    "closeApp",
    "exitApp",
    "closeAllTasks",
    "showPaywallAndCloseApp",
    "showErrorDialog",
    "logAndShowErrorDialog",
)

// Pairip retains these SDK names; class-scoped queries also cover overloaded methods.
internal fun DexClass.namedMethods(vararg names: String): List<MethodTarget> =
    klass(descriptor).methods { custom { name in names } }.all

internal fun noOp(classDef: DexClass, vararg methodNames: String): Int =
    classDef.namedMethods(*methodNames).onEach { it.alwaysReturn() }.size

internal fun returnTrue(classDef: DexClass, vararg methodNames: String): Int =
    classDef.namedMethods(*methodNames).onEach { it.alwaysReturn(true) }.size

internal fun forceLicenseResponseOk(classDef: DexClass): Int {
    if (classDef.methods.none { it.name == "processResponse" }) return 0

    klass(classDef.descriptor).method("processResponse").before {
        paramOfType(Type.Int).assign(int(0))
    }
    return 1
}

internal fun disableRepeatedCheck(classDef: DexClass): Int {
    if (classDef.field("repeatedCheckEnabled") == null) return 0
    if (classDef.methods.none { it.name == "<clinit>" }) return 0

    val client = klass(classDef.descriptor)
    client.method("<clinit>").before {
        setStatic(client.field("repeatedCheckEnabled"), bool(false))
    }
    return 1
}

// These optional checks live outside Pairip. Reject ambiguity instead of selecting an arbitrary app method.
private fun installerCheck(returnType: String): MethodTarget? {
    val matches = methods("installerCheck:$returnType") {
        strings(PLAY_STORE)
        params()
        returns(returnType)
        flags(AccessFlags.PRIVATE)
        custom { !owner.startsWith(PAIRIP_DESCRIPTOR_PREFIX) }
    }.all
    if (matches.isEmpty()) return null
    return matches.singleOrNull()
        ?: error("Pairip: multiple installer checks returning $returnType")
}

internal fun spoofInstallerChecks(): Int {
    val booleanCheck = installerCheck(Type.Boolean)
    val installerName = installerCheck(Type.String)
    booleanCheck?.alwaysReturn(true)
    installerName?.alwaysReturn(PLAY_STORE)
    return listOfNotNull(booleanCheck, installerName).size
}

internal val pairipStoreLaunchers = methods("pairipStoreLaunchers") {
    name("openPlayStore")
    custom { owner.startsWith(PAIRIP_DESCRIPTOR_PREFIX) }
}
