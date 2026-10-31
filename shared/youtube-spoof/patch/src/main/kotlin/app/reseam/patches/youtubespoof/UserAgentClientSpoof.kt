// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtubespoof

import app.reseam.patch.PatchRuntime
import app.reseam.patch.after
import app.reseam.patch.dex.stringValue
import app.reseam.patch.methods
import app.reseam.patch.point
import app.reseam.youtube.spoof.bindings.UserAgentClientSpoof

fun PatchRuntime.spoofUserAgentPackageName(stockPackageName: String) {
    val sites = userAgentPackageNameSites.all
    check(sites.isNotEmpty()) { "No user-agent package name sites" }
    sites.forEach {
        val packageName = it.point("user-agent package name") {
            invokeVirtual {
                owner("android.content.Context")
                name("getPackageName")
                params()
                returns("java.lang.String")
            }
        }.next { resultOf("java.lang.String") }
            .captureAs("packageName", "java.lang.String")
        packageName.after {
            capture("packageName").assign(
                UserAgentClientSpoof.rewritePackageName(capture("packageName"), string(stockPackageName)),
            )
        }
    }
}

private val userAgentPackageNameSites = methods("user-agent package name sites") {
    strings("(Linux; U; Android ")
    calls {
        owner("Landroid/content/Context;")
        name("getPackageName")
        params()
        returns("Ljava/lang/String;")
    }
    calls { owner("Ljava/lang/StringBuilder;"); name("append"); params("Ljava/lang/String;") }
    custom {
        instructions.none { it.stringValue == "android.resource://" || it.stringValue?.contains("gcore_") == true }
    }
}
