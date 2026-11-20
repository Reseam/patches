// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.meta

import app.reseam.meta.bindings.PushRegistration
import app.reseam.patch.CompatiblePackage
import app.reseam.patch.ReseamPatch
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.redirectTo

fun pushRegistrationFor(app: CompatiblePackage): ReseamPatch = patch(MetaPatchStrings.pushRegistration) {
    description(MetaPatchStrings.pushRegistrationDescription)
    compatibleWith(app)

    execute {
        replyIntent.redirectTo(PushRegistration.immutableBroadcast)
    }
}

private val replyIntent = method("instanceIdRequest") { strings("com.google.example.invalidpackage") }
    .point("replyIntent") { invokeStatic { owner("android.app.PendingIntent"); name("getBroadcast") } }
