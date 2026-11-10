// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.tiktok.privacy

import app.reseam.patch.ExtMethod
import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.tiktok.TikTokPatchStrings
import app.reseam.patches.tiktok.core.TIKTOK
import app.reseam.patches.tiktok.core.TikTokSettingsPages
import app.reseam.patches.tiktok.core.tiktokSettings
import app.reseam.tiktok.privacy.bindings.SimSpoof
import app.reseam.tiktok.settings.PrivacySettings
import app.reseam.tiktok.settings.TikTokStrings

private const val TELEPHONY_MANAGER = "android.telephony.TelephonyManager"

private val COUNTRIES =
    listOf(
        "ae",
        "au",
        "br",
        "ca",
        "ch",
        "de",
        "es",
        "fr",
        "gb",
        "hk",
        "id",
        "ie",
        "it",
        "jp",
        "kr",
        "mx",
        "my",
        "nl",
        "no",
        "nz",
        "ph",
        "pl",
        "pt",
        "sa",
        "se",
        "sg",
        "th",
        "tr",
        "tw",
        "us",
        "vn",
        "za",
    )

val spoofSim =
    patch(TikTokPatchStrings.spoofSim) {
        description(TikTokPatchStrings.spoofSimDescription)
        compatibleWith(TIKTOK)

        val defaultCountry =
            stringOption(
                "country",
                title = TikTokPatchStrings.spoofSimCountry,
                description = TikTokPatchStrings.spoofSimCountryDescription,
                default = "us",
                validValues = COUNTRIES,
            )

        settings(tiktokSettings) {
            listOf(
                section(
                    TikTokSettingsPages.Privacy,
                    TikTokStrings.sectionPrivacy,
                    PrivacySettings.spoofSim,
                    PrivacySettings.spoofSimCountry.copy(default = options[defaultCountry]),
                )
            )
        }

        execute {
            val reads: List<ExtMethod> =
                listOf(
                    SimSpoof.getSimCountryIso,
                    SimSpoof.getNetworkCountryIso,
                    SimSpoof.getSimOperator,
                    SimSpoof.getNetworkOperator,
                    SimSpoof.getSimOperatorName,
                    SimSpoof.getNetworkOperatorName,
                )
            val redirected = reads.sumOf { bytecode.redirectCalls(TELEPHONY_MANAGER, it.name, it) }
            check(redirected > 0) { "No TelephonyManager SIM reads to redirect" }
            log.info("Redirected $redirected SIM reads")
        }
    }
