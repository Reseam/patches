package app.reseam.patches.universal

import app.reseam.patch.BytecodeScope
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.DexClass
import app.reseam.patch.dex.isSet
import app.reseam.patch.patch

private const val PAIRIP_DESCRIPTOR_PREFIX = "Lcom/pairip/"
private const val SIGNATURE_CHECK = "com.pairip.SignatureCheck"
private const val VM_RUNNER = "com.pairip.VMRunner"
private const val STARTUP_LAUNCHER = "com.pairip.StartupLauncher"
private const val PAIRIP_APPLICATION = "com.pairip.application.Application"
private const val LICENSE_CLIENT = "com.pairip.licensecheck.LicenseClient"
private const val LICENSE_CLIENT_V3 = "com.pairip.licensecheck3.LicenseClientV3"
private const val LICENSE_CONTENT_PROVIDER = "com.pairip.licensecheck.LicenseContentProvider"
private const val LICENSE_ACTIVITY = "com.pairip.licensecheck.LicenseActivity"
private const val PLAY_STORE = "com.android.vending"

private val LICENSE_CLIENT_VOID_METHODS =
    arrayOf(
        "checkLicense",
        "initializeLicenseCheck",
        "connectToLicensingService",
        "retryOrThrow",
        "lambda\$retryOrThrow\$0",
        "startPaywallActivity",
        "startErrorDialogActivity",
        "scheduleAppShutdown",
    )

private val LICENSE_ACTIVITY_VOID_METHODS =
    arrayOf(
        "closeApp",
        "exitApp",
        "closeAllTasks",
        "showPaywallAndCloseApp",
        "showErrorDialog",
        "logAndShowErrorDialog",
    )

private fun noOp(
    classDef: DexClass,
    vararg methodNames: String,
): Int {
    var patched = 0
    for (methodName in methodNames) {
        classDef.methods.filter { it.name == methodName }.forEach {
            it.alwaysReturn()
            patched++
        }
    }
    return patched
}

private fun returnTrue(
    classDef: DexClass,
    vararg methodNames: String,
): Int {
    var patched = 0
    for (methodName in methodNames) {
        classDef.methods.filter { it.name == methodName }.forEach {
            it.alwaysReturn(true)
            patched++
        }
    }
    return patched
}

private fun forceLicenseResponseOk(classDef: DexClass): Int {
    val method = classDef.methods.firstOrNull { it.name == "processResponse" } ?: return 0
    val paramBase = method.registersSize - method.insSize
    val responseCode = if (method.isStatic) paramBase else paramBase + 1

    method.addInstructions(0) { const4(responseCode, 0) }
    return 1
}

private fun disableRepeatedCheck(classDef: DexClass): Int {
    val flag = classDef.field("repeatedCheckEnabled") ?: return 0
    val clinit = classDef.methods.firstOrNull { it.name == "<clinit>" } ?: return 0
    val scratch = clinit.registersSize

    if (!clinit.growLocalRegisters(1)) return 0

    clinit.addInstructions(0) {
        const4(scratch, 0)
        sputBoolean(scratch, flag)
    }
    return 1
}

private fun spoofInstallerChecks(scope: BytecodeScope): Int {
    var booleanDone = false
    var stringDone = false

    for (classDef in scope.classes) {
        if (classDef.descriptor.startsWith(PAIRIP_DESCRIPTOR_PREFIX)) continue

        for (method in classDef.methods) {
            if (method.parameterTypes.isNotEmpty()) continue
            if (method.indexOfFirstString(PLAY_STORE) == null) continue
            if (!AccessFlags.PRIVATE.isSet(method.info.accessFlags)) continue

            when (method.returnType) {
                "Z" -> {
                    if (!booleanDone) {
                        method.alwaysReturn(true)
                        booleanDone = true
                    }
                }

                "Ljava/lang/String;" -> {
                    if (!stringDone) {
                        method.alwaysReturn(PLAY_STORE)
                        stringDone = true
                    }
                }
            }
        }

        if (booleanDone && stringDone) break
    }

    return (if (booleanDone) 1 else 0) + (if (stringDone) 1 else 0)
}

val removePairip =
    patch("Remove Pairip") {
        description(
            "Removes Pairip (Google Play automatic integrity protection). " +
                "Optionally kills the Pairip VM. Does NOT bypass server-side Play " +
                "Integrity attestation or pairipcore virtualization.",
        )

        val killVM =
            boolOption(
                "killVM",
                title = "Kill Pairip VM",
                description =
                    "Also kills the Pairip VM. Only enable this for apps whose VM merely runs " +
                        "the startup integrity/license program. If the app routes real functionality " +
                        "through the VM enabling this breaks those features. Leave off unless the app needs it.",
                default = false,
            )

        val spoofInstaller =
            boolOption(
                "spoofInstaller",
                title = "Spoof installer checks",
                description =
                    "Rewrites leftover installer-origin checks that reference the Play Store package so they report a Play Store installation.",
                default = true,
            )

        execute {
            var patched = 0

            manifest.edit {
                findByTag("application").firstOrNull()?.let { application ->
                    if (application["android:name"] == PAIRIP_APPLICATION) {
                        bytecode
                            .findClass(PAIRIP_APPLICATION)
                            ?.superclass
                            ?.removePrefix("L")
                            ?.removeSuffix(";")
                            ?.replace('/', '.')
                            ?.let { original -> application["android:name"] = original }
                    }
                }

                findByAttribute("android:name", "com.google.android.play.core.common.PlayCoreDialogWrapperActivity").forEach { it.remove() }
                findByAttribute("android:name", LICENSE_ACTIVITY).forEach { it.remove() }
                findByAttribute("android:name", "com.android.vending.CHECK_LICENSE").forEach { it.remove() }

                for (tag in listOf("activity", "activity-alias", "service", "receiver", "provider", "meta-data")) {
                    findByTag(tag)
                        .filter { it["android:name"]?.startsWith("com.pairip.") == true }
                        .forEach { it.remove() }
                }
            }

            bytecode.findClass(LICENSE_CLIENT)?.let { client ->
                patched += noOp(client, *LICENSE_CLIENT_VOID_METHODS)
                patched += returnTrue(client, "performLocalInstallerCheck", "isIsolated")
                patched += forceLicenseResponseOk(client)
                patched += disableRepeatedCheck(client)
            }

            bytecode.findClass(LICENSE_CLIENT_V3)?.let { client ->
                patched += noOp(client, "onActivityCreate")
            }

            for (validator in listOf("com.pairip.licensecheck.ResponseValidator", "com.pairip.licensecheck.LicenseResponseHelper")) {
                bytecode.findClass(validator)?.let { classDef ->
                    classDef.methods.filter { it.name == "validateResponse" }.forEach {
                        if (it.returnType == "V") it.alwaysReturn() else it.alwaysReturn(true)
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
                wrapper.methods.filter { it.name == "attachBaseContext" }.forEach {
                    it.remove()
                    patched++
                }
            }

            for (classDef in bytecode.classes.filter { it.descriptor.startsWith(PAIRIP_DESCRIPTOR_PREFIX) }) {
                classDef.methods.filter { it.name == "openPlayStore" }.forEach {
                    it.alwaysReturn()
                    patched++
                }
            }

            if (options[killVM]) {
                bytecode.findClass(VM_RUNNER)?.let { vmRunner ->
                    patched += noOp(vmRunner, "<clinit>")

                    vmRunner.methods.filter { it.name == "invoke" }.forEach {
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
                patched += spoofInstallerChecks(bytecode)
            }

            if (patched == 0) {
                log.warn("Pairip: no Pairip methods found; no bytecode changes applied.")
            } else {
                log.info("Pairip: $patched method(s) neutralized.")
            }
        }
    }
