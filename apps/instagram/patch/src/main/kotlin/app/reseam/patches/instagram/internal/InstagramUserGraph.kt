// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.instagram.internal

import app.reseam.patch.Type
import app.reseam.patch.bind
import app.reseam.patch.classTarget
import app.reseam.patch.method

interface RuntimeUserPrincipal

/** How Instagram's user principal is reached from a media object, shared by the refs and download patches. */
object InstagramUserGraph {
    val shareUrlCarrier = method("shareUrlCarrier") {
        strings("https://www.instagram.com/p/", "unknown")
    }

    val principalFromMedia = bind<RuntimeUserPrincipal>("userPrincipalFromMedia") {
        fromField("mediaField") {
            owner(InstagramMediaGraph.feedClickHandler.owner)
            nearestObjectReadBeforeString("click_media_option")
        }
        fromMethod(shareUrlCarrier)
        raw {
            nextFieldRead(owner = sourceType)
            nextInterfaceCall(returningObject = true)
            nextFieldRead(owner = null)
        }
    }

    val principalClass = classTarget("userPrincipalClass") {
        bytecode.findClass(principalFromMedia.sourceType) ?: error("user principal class missing")
    }

    val usernameAccessor = method("userUsernameAccessor") {
        inClass(principalClass)
        calledBy(shareUrlCarrier)
        returns(Type.String)
        params()
    }

    val principal = bind<RuntimeUserPrincipal>("userPrincipal") {
        fromClass(principalClass)
        string("username") {
            callInterface(usernameAccessor.owner, usernameAccessor.name, usernameAccessor.proto)
        }
    }
}
