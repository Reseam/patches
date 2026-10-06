// SPDX-FileCopyrightText: 2026 MadKarma <me@madkarma.top>
// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:Suppress("unused")

package app.reseam.patches.universal

import app.reseam.patch.*

val removePairip = patch("Remove Pairip") {
    description(
        "Removes Pairip (Google Play automatic integrity protection). Optionally kills the Pairip VM. Does NOT bypass server-side Play Integrity attestation or pairipcore virtualization.",
    )

    val killVM = boolOption(
        "killVM",
        title = "Kill Pairip VM",
        description = "Also kills the Pairip VM. Only enable this for apps whose VM merely runs the startup integrity/license program. If the app routes real functionality through the VM enabling this breaks those features. Leave off unless the app needs it.",
        default = false,
    )

    val spoofInstaller = boolOption(
        "spoofInstaller",
        title = "Spoof installer checks",
        description = "Rewrites leftover installer-origin checks that reference the Play Store package so they report a Play Store installation.",
        default = true,
    )

    execute {
        var patched = 0

        manifest.edit {
            findByTag("application").firstOrNull()?.let { application ->
                if (application["android:name"] == PAIRIP_APPLICATION) {
                    val original = bytecode.findClass(PAIRIP_APPLICATION)?.superclass
                        ?: error("Pairip: application wrapper has no superclass")
                    application["android:name"] = className(original)
                }
            }

            findByAttribute(
                "android:name", "com.google.android.play.core.common.PlayCoreDialogWrapperActivity"
            ).forEach { it.remove() }
            findByAttribute(
                "android:name", LICENSE_ACTIVITY
            ).forEach { it.remove() }
            findByAttribute(
                "android:name", "com.android.vending.CHECK_LICENSE"
            ).forEach { it.remove() }

            for (tag in listOf(
                "activity", "activity-alias", "service", "receiver", "provider", "meta-data"
            )) {
                findByTag(tag).filter { it["android:name"]?.startsWith("com.pairip.") == true }
                    .forEach { it.remove() }
            }
        }

        bytecode.findClass(LICENSE_CLIENT)?.let { client ->
            patched += noOp(client, *LICENSE_CLIENT_VOID_METHODS)
            patched += returnTrue(
                client, "performLocalInstallerCheck", "isIsolated"
            )
            patched += forceLicenseResponseOk(client)
            patched += disableRepeatedCheck(client)
        }

        bytecode.findClass(LICENSE_CLIENT_V3)?.let { client ->
            patched += noOp(client, "onActivityCreate")
        }

        for (validator in listOf(
            "com.pairip.licensecheck.ResponseValidator",
            "com.pairip.licensecheck.LicenseResponseHelper"
        )) {
            bytecode.findClass(validator)?.let { classDef ->
                classDef.namedMethods("validateResponse").forEach {
                    if (it.returnType == Type.Void) it.alwaysReturn() else it.alwaysReturn(true)
                    patched++
                }
            }
        }

        bytecode.findClass(LICENSE_CONTENT_PROVIDER)?.let { provider ->
            patched += returnTrue(provider, "onCreate")
        }

        bytecode.findClass(LICENSE_ACTIVITY)?.let { activity ->
            patched += noOp(activity, *LICENSE_ACTIVITY_VOID_METHODS)
        }

        bytecode.findClass(SIGNATURE_CHECK)?.let { signatureCheck ->
            patched += noOp(signatureCheck, "verifyIntegrity")
            patched += returnTrue(signatureCheck, "verifySignatureMatches")
        }

        bytecode.findClass(PAIRIP_APPLICATION)?.let { wrapper ->
            wrapper.namedMethods("attachBaseContext").forEach {
                it.method.remove()
                patched++
            }
        }

        pairipStoreLaunchers.forEach {
            alwaysReturn()
            patched++
        }

        if (options[killVM]) {
            bytecode.findClass(VM_RUNNER)?.let { vmRunner ->
                patched += noOp(vmRunner, "<clinit>")

                vmRunner.namedMethods("invoke").forEach {
                    it.alwaysReturnNull()
                    patched++
                }

                patched += returnTrue(vmRunner, "isDebuggingEnabled")
            }

            bytecode.findClass(STARTUP_LAUNCHER)?.let { startupLauncher ->
                patched += noOp(startupLauncher, "launch", "pairip")
            }
        }

        if (options[spoofInstaller]) {
            patched += spoofInstallerChecks()
        }

        if (patched == 0) {
            log.warn("Pairip: no Pairip methods found; no bytecode changes applied.")
        } else {
            log.info("Pairip: $patched method(s) neutralized.")
        }
    }
}
