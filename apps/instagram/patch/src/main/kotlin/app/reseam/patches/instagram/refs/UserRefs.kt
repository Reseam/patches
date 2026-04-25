// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.instagram.refs

import app.reseam.patch.PatchContext
import app.reseam.patch.bind
import app.reseam.patch.bindStub
import app.reseam.patch.classHandle
import app.reseam.patch.compatibleWith
import app.reseam.patch.findMethod
import app.reseam.patch.patch
import app.reseam.patches.instagram.core.signatureCheckPatch

private const val REFS_USER = "Lapp/reseam/instagram/refs/User;"
private const val MEDIA_OPTION_TYPE = "Lcom/instagram/feed/media/mediaoption/MediaOption\$Option;"

private interface RuntimeUserPrincipal

val userRefs = patch(
    name = "User refs",
    description = "Internal: binds app.reseam.instagram.refs.User bridges to Instagram's user principal.",
    compatibleWith = listOf(compatibleWith("com.instagram.android")),
    dependsOn = listOf(signatureCheckPatch),
    enabledByDefault = false,
) {
    extendWith("instagram-refs.dex")

    execute { ctx ->
        val feedClickHandler = ctx.findMethod(debug = "feedClickHandler") {
            strings("click_media_option", "MediaOptionsOverflowHelper")
            returnType("V")
            parameterTypes(MEDIA_OPTION_TYPE)
        }
        val shareUrlCarrier = shareUrlCarrierMethod(ctx)

        val principalFromMedia = ctx.bind<RuntimeUserPrincipal>(debug = "userPrincipalFromMedia") {
            fromField(debug = "mediaField") {
                owner(feedClickHandler.classDescriptor)
                nearestObjectReadBeforeString("click_media_option")
            }

            fromMethod(debug = "shareUrlCarrier") {
                sameAs(shareUrlCarrier)
            }

            raw {
                nextFieldRead(owner = sourceType)
                nextInterfaceCall(returningObject = true)
                nextFieldRead(owner = null)
            }
        }

        val principalClass = ctx.classHandle(principalFromMedia.sourceType, debug = "userPrincipalClass")
        val usernameAccessor = ctx.findMethod(debug = "userUsernameAccessor") {
            inClass(principalClass)
            calledBy(shareUrlCarrier)
            returnType("Ljava/lang/String;")
            parameterTypes()
        }

        val principal = ctx.bind<RuntimeUserPrincipal>(debug = "userPrincipal") {
            fromClass(principalClass)

            string("username") {
                callInterface(
                    usernameAccessor.classDescriptor,
                    usernameAccessor.methodName,
                    usernameAccessor.proto,
                )
            }
        }

        ctx.bindStub(REFS_USER) {
            method("fromMedia", "(Ljava/lang/Object;)Ljava/lang/Object;") {
                returnObject(principalFromMedia.of(parameter(0)))
            }

            method("username", "(Ljava/lang/Object;)Ljava/lang/String;") {
                returnObject(principal.member("username", parameter(0)))
            }
        }

        ctx.log.info(
            "User refs bound through feedClickHandler media anchor and shareUrlCarrier chain"
        )
    }
}

private fun shareUrlCarrierMethod(ctx: PatchContext) =
    ctx.findMethod(debug = "shareUrlCarrier") {
        strings("https://www.instagram.com/p/", "unknown")
    }
