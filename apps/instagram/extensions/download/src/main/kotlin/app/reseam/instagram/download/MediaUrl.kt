// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.instagram.download

import app.reseam.bundle.MediaKind
import app.reseam.instagram.refs.photoUrl
import app.reseam.instagram.refs.videoUrl

internal enum class MediaType(val extension: String, val mimeType: String, val kind: MediaKind) {
    VIDEO(".mp4", "video/mp4", MediaKind.VIDEO),
    PHOTO(".jpg", "image/jpeg", MediaKind.IMAGE),
}

internal data class MediaUrl(val url: String, val type: MediaType)

internal fun bestMediaUrl(media: Any): MediaUrl? =
    videoUrl(media)?.takeIf { it.isNotEmpty() }?.let { MediaUrl(it, MediaType.VIDEO) }
        ?: photoUrl(media)?.takeIf { it.isNotEmpty() }?.let { MediaUrl(it, MediaType.PHOTO) }
