// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.tiktok.privacy

import app.reseam.patch.Type
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.methodRef
import app.reseam.patch.dex.parameterTypes
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
import app.reseam.tiktok.settings.PrivacySettings
import app.reseam.tiktok.settings.TikTokStrings

private const val COROUTINE_CONTEXT = "Lkotlin/coroutines/CoroutineContext;"
private val TELEMETRY_CALLS =
    setOf("onEvent", "onEventV3", "onMiscEvent", "onInternalEventV3", "flush", "flushAsync")

val privacy =
    patch(TikTokPatchStrings.privacy) {
        description(TikTokPatchStrings.privacyDescription)
        compatibleWith(TIKTOK)
        settings(
            tiktokSettings,
            section(
                TikTokSettingsPages.Privacy,
                TikTokStrings.sectionPrivacy,
                PrivacySettings.disableTelemetry,
                PrivacySettings.disableWatchHistory,
                PrivacySettings.hideTypingIndicator,
                PrivacySettings.disableSearchHistory,
            ),
        )

        execute {
            gate(PrivacySettings.disableTelemetry) {
                telemetry.all
                    .also { check(it.isNotEmpty()) { "No AppLog event calls" } }
                    .forEach { it.alwaysReturn() }
            }
            gate(PrivacySettings.disableWatchHistory) {
                watchReports.all
                    .also {
                        check(it.size == 2) { "Expected two watch report calls, found ${it.size}" }
                    }
                    .forEach { it.alwaysReturn() }
            }
            gate(PrivacySettings.hideTypingIndicator) { sendTyping.alwaysReturn() }
            gate(PrivacySettings.disableSearchHistory) {
                saveHistory.alwaysReturn()
                recordSearch.alwaysReturn()
            }
        }
    }

private val telemetry =
    klass("com.bytedance.applog.AppLog").methods("telemetry") {
        flags(AccessFlags.STATIC)
        returns(Type.Void)
        custom { name in TELEMETRY_CALLS }
    }

private val watchReports =
    klass("com.ss.android.ugc.aweme.feed.api.AwemeStatsApi").methods("watchReports") {
        flags(AccessFlags.STATIC)
        paramCount(1)
        returns(Type.Void)
    }

private val sendTyping =
    method("sendTyping") {
        inClass(klass("com.ss.android.ugc.aweme.im.typingindicator.timer.TypingStatusSenderTimer"))
        params(Type.String)
        returns(Type.Void)
        custom { instructions.any { COROUTINE_CONTEXT in it.methodRef?.parameterTypes.orEmpty() } }
    }

private val saveHistory =
    method("saveHistory") {
        strings("block search history, source:")
        params("com.ss.android.ugc.aweme.search.model.SearchHistory", Type.String)
        returns(Type.Void)
    }

private val recordSearch =
    method("recordSearch") {
        inClass(
            klass("com.ss.android.ugc.aweme.search.pages.middlepage.history.ManualSearchPvStore")
        )
        flags(AccessFlags.STATIC)
        params(Type.String, Type.String)
        returns(Type.Void)
    }
