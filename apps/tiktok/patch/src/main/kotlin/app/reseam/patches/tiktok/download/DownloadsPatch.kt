// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.tiktok.download

import app.reseam.patch.Type
import app.reseam.patch.dex.Opcode
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.tiktok.TikTokPatchStrings
import app.reseam.patches.tiktok.core.AWEME
import app.reseam.patches.tiktok.core.TIKTOK
import app.reseam.patches.tiktok.core.TikTokSettingsPages
import app.reseam.patches.tiktok.core.tiktokSettings
import app.reseam.tiktok.download.bindings.DownloadPermission
import app.reseam.tiktok.settings.DownloadSettings
import app.reseam.tiktok.settings.TikTokStrings

private const val URL_MODEL = "com.ss.android.ugc.aweme.base.model.UrlModel"
private const val ACL_COMMON_SHARE = "com.ss.android.ugc.aweme.feed.model.ACLCommonShare"

val downloads =
    patch(TikTokPatchStrings.downloads) {
        description(TikTokPatchStrings.downloadsDescription)
        compatibleWith(TIKTOK)
        settings(
            tiktokSettings,
            section(
                TikTokSettingsPages.Downloads,
                TikTokStrings.sectionDownloads,
                DownloadSettings.removeWatermark,
                DownloadSettings.downloadAnyPost,
            ),
        )

        execute {
            val sourceUrl =
                cleanSource
                    .point {
                        opcode(Opcode.IPUT_OBJECT)
                        field { type(URL_MODEL) }
                    }
                    .field("sourceUrl")
            gate(DownloadSettings.removeWatermark) {
                watermarkedSource.before {
                    thisObject.call(cleanSource, param(0), bool(true))
                    whenNotNull(thisObject.field(sourceUrl)) { returnVoid() }
                }
                imageWatermarks.all
                    .also {
                        check(it.size == 4) {
                            "Expected four image watermark steps, found ${it.size}"
                        }
                    }
                    .forEach { it.alwaysReturn() }
                downloadPermission.after { returnValue(DownloadPermission.untranscoded(result)) }
            }
            gate(DownloadSettings.downloadAnyPost) {
                downloadPermission.after { returnValue(DownloadPermission.allow(result)) }
                preventDownload.alwaysReturn(false)
            }
        }
    }

private val cleanSource =
    method("cleanSource") {
        strings("tag_no_water")
        params(AWEME, Type.Boolean)
        returns(Type.Void)
    }

private val watermarkedSource =
    method("watermarkedSource") {
        inClass(klass(cleanSource.owner))
        params(AWEME)
        returns(Type.Void)
    }

private val imageWatermarks =
    klass("com.ss.android.ugc.aweme.watermark.WaterMarkServiceImpl").methods("imageWatermarks") {
        returns(Type.Void)
        custom { name.startsWith("add") && name.endsWith("ToImage") }
    }

private val downloadPermission =
    klass("com.ss.android.ugc.aweme.feed.model.AwemeACLShare").method("getDownloadGeneral") {
        returns(ACL_COMMON_SHARE)
    }

private val preventDownload = klass(AWEME).method("isPreventDownload") { returns(Type.Boolean) }
