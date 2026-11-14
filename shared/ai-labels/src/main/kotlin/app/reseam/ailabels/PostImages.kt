// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.ailabels

import android.util.LruCache

/** The images of posts with a label, by link: images outside posts are never checked. */
internal object PostImages : LruCache<String, PostImage>(2_000) {
    fun add(image: PostImage) {
        put(image.url, image)
    }
}
