// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.tiktok.account

import app.reseam.patch.Type
import app.reseam.patch.alwaysReturn
import app.reseam.patch.classTarget
import app.reseam.patch.field
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patches.tiktok.core.TIKTOK

// Google Play services and the Facebook app both check TikTok's signing certificate, and refuse
// TikTok's sign-in for any certificate but TikTok's own. The browser sign-in TikTok falls back to
// cannot see the certificate.
val fixLogin =
    patch("Fix login") {
        description("Makes logging in with Google and Facebook work.")
        compatibleWith(TIKTOK)

        execute {
            googlePlayServicesAvailable.alwaysReturn(false)
            facebookAppAllowed.alwaysReturn(false)
            facebookAppTokenAllowed.alwaysReturn(false)
        }
    }

private val googlePlayServicesAvailable =
    klass("com.bytedance.lobby.google.GoogleAuth").method("isAvailable") {
        returns(Type.Boolean)
    }

private val loginRequestBehavior =
    klass("com.facebook.login.LoginClient\$Request").field("loginBehavior")

private val facebookLoginBehavior =
    classTarget("facebookLoginBehavior") {
        bytecode.findClass(loginRequestBehavior.type)
            ?: error("The Facebook login behavior is missing")
    }

private val facebookAppAllowed =
    facebookLoginBehavior.method("allowsKatanaAuth") { returns(Type.Boolean) }

private val facebookAppTokenAllowed =
    facebookLoginBehavior.method("allowsGetTokenAuth") { returns(Type.Boolean) }
