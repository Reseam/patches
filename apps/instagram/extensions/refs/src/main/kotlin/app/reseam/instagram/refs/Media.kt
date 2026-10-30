// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("Media")

package app.reseam.instagram.refs

import app.reseam.runtime.Export

@Export
fun photoUrl(media: Any): String? =
    imageCandidates(media)?.firstNotNullOfOrNull { candidate ->
        imageCandidateUrl(candidate)?.takeIf { it.isNotEmpty() }
    }

@Export external fun imageCandidates(media: Any): List<Any>?

@Export external fun imageCandidateUrl(candidate: Any): String?

@Export external fun videoUrl(media: Any): String?

@Export external fun children(media: Any): List<*>?
