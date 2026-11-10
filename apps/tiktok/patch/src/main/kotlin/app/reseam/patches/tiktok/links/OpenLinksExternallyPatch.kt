// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.tiktok.links

import app.reseam.patch.Type
import app.reseam.patch.before
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.field
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.tiktok.TikTokPatchStrings
import app.reseam.patches.tiktok.core.TIKTOK
import app.reseam.patches.tiktok.core.TikTokSettingsPages
import app.reseam.patches.tiktok.core.tiktokSettings
import app.reseam.tiktok.links.bindings.ExternalLinks
import app.reseam.tiktok.settings.LinkSettings
import app.reseam.tiktok.settings.TikTokStrings

private const val SPARK_THIRD_CONTEXT = "com.bytedance.hybrid.spark.third.router.SparkThirdContext"

private val smartRoute = klass("com.bytedance.router.SmartRoute")

val openLinksExternally =
    patch(TikTokPatchStrings.openLinksExternally) {
        description(TikTokPatchStrings.openLinksExternallyDescription)
        compatibleWith(TIKTOK)
        settings(
            tiktokSettings,
            section(
                TikTokSettingsPages.Downloads,
                TikTokStrings.sectionLinks,
                LinkSettings.openLinksExternally,
            ),
        )

        execute {
            val context = smartRoute.field("mContext")
            val url = smartRoute.field("mUrl")
            val data = smartRoute.field("mData")
            val pageUrl = klass(SPARK_THIRD_CONTEXT).field("url")
            gate(LinkSettings.openLinksExternally) {
                routeOpens.all
                    .also { check(it.isNotEmpty()) { "No SmartRoute.open methods" } }
                    .forEach {
                        it.before {
                            whenTrue(
                                call(
                                    ExternalLinks.open,
                                    thisObject.field(context),
                                    thisObject.field(url),
                                    thisObject.field(data),
                                )
                            ) {
                                returnVoid()
                            }
                        }
                    }
                // Bio links, search landing pages and the in-app browser open third-party pages
                // here.
                thirdPartyPage.before {
                    whenTrue(call(ExternalLinks.openPage, param(0), param(1).field(pageUrl))) {
                        returnVoid()
                    }
                }
            }
        }
    }

private val routeOpens = smartRoute.methods("routeOpens") { name("open") }

private val thirdPartyPage =
    method("thirdPartyPage") {
        strings("ContainerId")
        flags(AccessFlags.STATIC)
        params("android.content.Context", SPARK_THIRD_CONTEXT)
        returns(Type.Void)
    }
