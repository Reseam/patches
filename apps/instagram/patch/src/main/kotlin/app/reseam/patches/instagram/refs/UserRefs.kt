// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.instagram.refs

import app.reseam.patch.patch
import app.reseam.patches.instagram.core.INSTAGRAM
import app.reseam.patches.instagram.core.UserRefs
import app.reseam.patches.instagram.core.signatureCheck
import app.reseam.patches.instagram.internal.InstagramUserGraph

val userRefs = patch {
    description("Binds app.reseam.instagram.refs.User bridges to Instagram's user principal.")
    compatibleWith(INSTAGRAM)
    dependsOn(signatureCheck)

    execute {
        UserRefs.fromMedia.implement {
            returnValue(InstagramUserGraph.principalFromMedia.of(param(0)))
        }
        UserRefs.username.implement {
            returnValue(InstagramUserGraph.principal.member("username", param(0)))
        }
    }
}
