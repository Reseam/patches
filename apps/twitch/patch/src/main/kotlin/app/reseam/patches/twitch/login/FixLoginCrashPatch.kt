// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.twitch.login

import app.reseam.patch.MethodTarget
import app.reseam.patch.PatchRuntime
import app.reseam.patch.Type
import app.reseam.patch.before
import app.reseam.patch.dex.stringValue
import app.reseam.patch.field
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patches.twitch.core.TWITCH
import app.reseam.twitch.login.bindings.LoginPage

private const val LOGIN_SUCCESS = "login-success"

val fixLoginCrash = patch("Fix login crash") {
    description("Stops Twitch from crashing right after you log in.")
    compatibleWith(TWITCH)

    execute {
        val handlers = loginPageHandlers.all
        check(handlers.isNotEmpty()) { "Twitch's login screens no longer handle a $LOGIN_SUCCESS event" }

        handlers.forEach { handler ->
            val serializerCall = handler.point("serializerCall") { string(LOGIN_SUCCESS) }.next { invokeVirtual { name("serializer") } }
            val names = loginResultNames(serializerCall.callee("serializerGetter"))
            check(names.size > 1) { "The login result's serializer names no fields" }
            val resultClass = names.first()
            // Another class's fields would drop the login token.
            check(resultClass.endsWith(".LoginPageResponse")) { "Twitch now parses the login result as $resultClass" }
            val declaredFields = names.drop(1)

            serializerCall
                .next { invokeVirtual { paramCount(2); hasParam(Type.String); returns(Type.Object) } }
                .captureArgumentAs("loginPageResult", 2, Type.String)
                .before {
                    capture("loginPageResult").assign(LoginPage.declaredFieldsOnly(capture("loginPageResult"), string(declaredFields.joinToString(","))))
                }
            log.info("$resultClass keeps only ${declaredFields.joinToString()}")
        }
    }
}

// The serializer's static initializer names the class, then each field Twitch reads.
private fun PatchRuntime.loginResultNames(serializerGetter: MethodTarget): List<String> {
    val serializer = bytecode.findClass(serializerGetter.field("serializerInstance") { }.owner) ?: error("The login result has no serializer class")
    val initializer = serializer.directMethods.singleOrNull { it.name == "<clinit>" } ?: error("The login result's serializer no longer has a static initializer")
    return initializer.instructions.mapNotNull { it.stringValue }
}

private val loginPageHandlers = methods("loginPageHandlers") { strings(LOGIN_SUCCESS) }
