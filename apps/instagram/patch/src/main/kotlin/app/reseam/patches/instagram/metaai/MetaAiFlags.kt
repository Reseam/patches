// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.instagram.metaai

import app.reseam.patch.MethodTarget

internal data class MetaAiFlags(val exploreSearch: Long, val searchHint: Long, val directSearch: List<Long>)

// MobileConfig ids change between builds; the DM search hint picks the build's set.
internal fun metaAiFlags(searchView: MethodTarget): MetaAiFlags =
    flagSets.single { searchView.method.containsLiteral(it.searchHint) }

private val flagSets = listOf(
    MetaAiFlags( // 447.0.0.55.81
        exploreSearch = 0x0081069d00111fe8L,
        searchHint = 0x00810c29000a438cL,
        directSearch = listOf(
            0x00810420000b119dL, // send-to-AI arrow
            0x008104200004119aL, // suggestions while typing
            0x0081042000001199L, // prompts on an empty query
        ),
    ),
    MetaAiFlags( // 451.0.0.53.80
        exploreSearch = 0x0081067900111f10L,
        searchHint = 0x00810bf7000a42a1L,
        directSearch = listOf(
            0x00810411000b10fdL, // send-to-AI arrow
            0x00810411000410faL, // suggestions while typing
            0x00810411000010f9L, // prompts on an empty query
        ),
    ),
)
