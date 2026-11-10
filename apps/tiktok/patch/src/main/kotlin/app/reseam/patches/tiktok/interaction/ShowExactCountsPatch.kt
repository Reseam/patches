// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.tiktok.interaction

import app.reseam.patch.Type
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.enumConstant
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
import app.reseam.tiktok.interaction.bindings.Counts
import app.reseam.tiktok.settings.InterfaceSettings
import app.reseam.tiktok.settings.TikTokStrings

val showExactCounts =
    patch(TikTokPatchStrings.showExactCounts) {
        description(TikTokPatchStrings.showExactCountsDescription)
        compatibleWith(TIKTOK)
        settings(
            tiktokSettings,
            section(
                TikTokSettingsPages.Video,
                TikTokStrings.sectionOverlay,
                InterfaceSettings.showExactCounts,
            ),
        )

        execute {
            val formatters = compactCounts("1.0M").all + compactCounts("1M").all
            check(formatters.size == 4) {
                "Expected four count formatters, found ${formatters.size}"
            }
            gate(InterfaceSettings.showExactCounts) {
                formatters.forEach { it.before { returnValue(call(Counts.exact, param(0))) } }
                formatOptions.before {
                    whenEqual(param(1), staticField(notation.enumConstant("COMPACT"))) {
                        param(1).assign(staticField(notation.enumConstant("STANDARD")))
                    }
                }
            }
        }
    }

// Newer screens, the feed buttons included, format through TikTok's number formatter instead.
private val notation =
    klass("numberNotation") { enumConstants("STANDARD", "SCIENTIFIC", "COMPACT", "DYNAMIC") }

private val formatOptions =
    klass("numberFormatOptions") { hasInstanceField(notation.descriptor) }
        .method("<init>") { paramCount(4) }

private fun compactCounts(million: String) =
    methods("compactCounts") {
        strings(million, "K", "M", "B")
        flags(AccessFlags.STATIC)
        params(Type.Long)
        returns(Type.String)
    }
