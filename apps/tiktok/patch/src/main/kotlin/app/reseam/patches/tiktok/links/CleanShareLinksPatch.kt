// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.tiktok.links

import app.reseam.patch.MethodTarget
import app.reseam.patch.Type
import app.reseam.patch.before
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.tiktok.core.TIKTOK
import app.reseam.patches.tiktok.core.TikTokSettingsPages
import app.reseam.patches.tiktok.core.tiktokSettings
import app.reseam.tiktok.links.bindings.ShareLinks
import app.reseam.tiktok.settings.LinkSettings
import app.reseam.tiktok.settings.TikTokStrings

private const val SHARE_PACKAGE = "com.ss.android.ugc.aweme.share.base.model.BaseSharePackage"
private const val SHARE_SERVICE = "com.ss.android.ugc.aweme.share.ShareExtService"

val cleanShareLinks =
    patch("Clean share links") {
        description(
            "Shares plain tiktok.com links, without tracking parameters or short links that identify you."
        )
        compatibleWith(TIKTOK)
        settings(
            tiktokSettings,
            section(
                TikTokSettingsPages.Downloads,
                TikTokStrings.sectionLinks,
                LinkSettings.cleanShareLinks,
            ),
        )

        execute {
            gate(LinkSettings.cleanShareLinks) {
                mapOf(shareLink to 0, shareLinkTask to 2).forEach { (builder, url) ->
                    builder.before { param(url).assign(call(ShareLinks.clean, param(url))) }
                    // The builder's first check returns the link as given, before parameters and
                    // shortening.
                    builder.plainLinkCheck().captureAs("plain").after {
                        capture("plain").assign(bool(true))
                    }
                }
                // A cached short link from an earlier share would skip the builder.
                cachedShareLink.before {
                    returnValue(call(shareLink, param(0), param(1), param(2), param(3), param(4)))
                }
            }
        }
    }

private fun MethodTarget.plainLinkCheck() = point {
    invokeStatic {
        paramCount(0)
        returns(Type.Boolean)
    }
}.next { resultOf(Type.Boolean) }

private val shareLink =
    method("shareLink") {
        flags(AccessFlags.STATIC)
        params(Type.String, Type.String, Type.String, Type.Int, SHARE_PACKAGE)
        returns(Type.String)
        calls { owner(SHARE_SERVICE) }
    }

private val cachedShareLink =
    method("cachedShareLink") {
        flags(AccessFlags.STATIC)
        params(Type.String, Type.String, Type.String, Type.Int, SHARE_PACKAGE)
        returns(Type.String)
        calls(shareLink)
    }

private val shareLinkTask =
    method("shareLinkTask") {
        flags(AccessFlags.STATIC)
        hasParam(SHARE_PACKAGE)
        paramCount(7)
        calls { owner(SHARE_SERVICE) }
        custom { parameterTypes[0] == classDef.descriptor && parameterTypes[2] == Type.String }
    }
