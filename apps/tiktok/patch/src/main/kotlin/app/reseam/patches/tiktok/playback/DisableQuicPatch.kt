// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.tiktok.playback

import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.before
import app.reseam.patch.enumConstant
import app.reseam.patch.field
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.tiktok.TikTokPatchStrings
import app.reseam.patches.tiktok.core.TIKTOK
import app.reseam.patches.tiktok.core.TikTokSettingsPages
import app.reseam.patches.tiktok.core.tiktokSettings
import app.reseam.tiktok.settings.PlaybackSettings
import app.reseam.tiktok.settings.TikTokStrings

private const val LOADER = "com.ss.mediakit.medialoader"

val disableQuic =
    patch(TikTokPatchStrings.disableQuic) {
        description(TikTokPatchStrings.disableQuicDescription)
        compatibleWith(TIKTOK)
        settings(
            tiktokSettings,
            section(
                TikTokSettingsPages.Playback,
                TikTokStrings.sectionPlayback,
                PlaybackSettings.disableQuic,
            ),
        )

        execute {
            val configure = klass("$LOADER.AVMDLDataLoaderConfigure")
            val quicLoader = configure.field("mEnableTTQuicHeLoader")
            val quicLoaderByNetwork = configure.field("mEnableTTQuicHeLoaderCheckWithNetworkType")
            val transport = klass(transportMode.returnType)
            val quic = transport.enumConstant("QUIC")
            val tls = transport.enumConstant("TLS")
            gate(PlaybackSettings.disableQuic) {
                applyLoaderConfig.before {
                    param(0).set(quicLoader, int(0))
                    param(0).set(quicLoaderByNetwork, int(0))
                }
                transportMode.after {
                    whenEqual(result, staticField(quic)) { returnValue(staticField(tls)) }
                }
                uploaderQuic.alwaysReturn(false)
            }
        }
    }

private val applyLoaderConfig = klass("$LOADER.AVMDLDataLoader").method("setConfigureInternal")

private val transportMode =
    klass("org.chromium.wschannel.CronetFrontierConnection").method("covertTransportMode")

private val uploaderQuic =
    klass("com.ss.bduploader.BDUploadUtil").method("xquicAvalilable") { returns(Type.Boolean) }
