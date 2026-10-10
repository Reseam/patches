// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtubespoof

import app.reseam.patch.ExtClass
import app.reseam.patch.PatchRuntime
import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.dex.stringValue
import app.reseam.patch.methods
import app.reseam.patch.point

private object UserAgentClientSpoof : ExtClass("app.reseam.youtube.spoof.UserAgentClientSpoof") {
    val rewritePackageName by static(Type.String, Type.String, returns = Type.String)
}

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
                call(UserAgentClientSpoof.rewritePackageName, capture("packageName"), string(stockPackageName)),
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
