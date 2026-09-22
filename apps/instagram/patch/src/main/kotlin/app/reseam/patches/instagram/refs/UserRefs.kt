// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.instagram.refs

import app.reseam.patch.Type
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patches.instagram.core.INSTAGRAM
import app.reseam.patches.instagram.core.UserRefs
import app.reseam.patches.instagram.core.signatureCheck

private const val MEDIA = "com.instagram.feed.media.Media"
private const val USER = "com.instagram.user.model.User"

private val mediaUser = method("mediaUser") {
    inClass(klass(MEDIA))
    strings("user")
    returns(USER)
    params()
}

private val userUsername = method("userUsername") {
    inClass(klass(USER))
    literals("username".hashCode().toLong())
    returns(Type.String)
    params()
}

val userRefs = patch {
    description("Binds app.reseam.instagram.refs.User bridges to Instagram's user principal.")
    compatibleWith(INSTAGRAM)
    dependsOn(signatureCheck)

    execute {
        UserRefs.fromMedia.implement {
            whenNotNull(param(0)) {
                returnValue(param(0).cast(MEDIA).call(mediaUser))
            }
            returnNull()
        }
        UserRefs.username.implement {
            whenNotNull(param(0)) {
                returnValue(param(0).cast(USER).call(userUsername))
            }
            returnNull()
        }
    }
}
