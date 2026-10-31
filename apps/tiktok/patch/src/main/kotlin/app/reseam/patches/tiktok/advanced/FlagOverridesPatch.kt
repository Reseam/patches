// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.tiktok.advanced

import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.section
import app.reseam.patches.tiktok.core.TIKTOK
import app.reseam.patches.tiktok.core.TikTokSettingsPages
import app.reseam.patches.tiktok.core.tiktokSettings
import app.reseam.tiktok.advanced.bindings.FlagOverrides
import app.reseam.tiktok.settings.AdvancedSettings
import app.reseam.tiktok.settings.TikTokStrings

private val OVERRIDES =
    mapOf(
        Type.Boolean to FlagOverrides.booleanOverride,
        Type.Int to FlagOverrides.intOverride,
        Type.Long to FlagOverrides.longOverride,
        Type.String to FlagOverrides.stringOverride,
    )

val flagOverrides =
    patch("Flag overrides") {
        description("Lets you set TikTok's server settings and experiments by key, for testing.")
        compatibleWith(TIKTOK)
        settings(
            tiktokSettings,
            section(
                TikTokSettingsPages.Advanced,
                TikTokStrings.sectionAdvanced,
                AdvancedSettings.flagOverrides,
            ),
        )

        execute {
            val getters = settingsGetters.all + experimentGetters().all
            check(getters.isNotEmpty()) { "No flag getters" }
            getters.forEach { getter ->
                val key = getter.parameterTypes.indexOf(Type.String)
                getter.after {
                    returnValue(call(OVERRIDES.getValue(getter.returnType), param(key), result))
                }
            }
        }
    }

private val settingsGetters =
    klass("com.bytedance.ies.abmock.SettingsManager").methods("settingsGetters") {
        flags(AccessFlags.STATIC)
        param(0, Type.String)
        paramCount(2)
        custom { returnType in OVERRIDES }
    }

private fun experimentGetters() =
    klass(
            klass("com.ss.android.ugc.aweme.feed.assem.videoauthorinfo.VideoAuthorInfoVM")
                .method("paramSync2StateAccept")
                .point { string("show_play_count") }
                .next { invokeVirtual { hasParam(Type.String) } }
                .callee()
                .owner
        )
        .methods("experimentGetters") {
            custom {
                !isStatic && returnType in OVERRIDES && "Ljava/lang/String;" in parameterTypes
            }
        }
