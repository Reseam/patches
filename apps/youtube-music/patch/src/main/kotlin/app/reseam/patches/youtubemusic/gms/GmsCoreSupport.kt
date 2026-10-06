// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtubemusic.gms

import app.reseam.patches.gmscore.gmsCoreSupportFor
import app.reseam.patches.youtubemusic.core.YOUTUBE_MUSIC
import app.reseam.patches.youtubemusic.core.musicActivityOnCreate

// The certificate the app shipped with before Google rotated its key; servers still know it by it.
val gmsCoreSupport = gmsCoreSupportFor(
    YOUTUBE_MUSIC,
    signature = "afb0fed5eeaebdd86f56a97742f4b6b33ef59875",
    defaultPackageName = "app.reseam.android.apps.youtube.music",
    musicActivityOnCreate,
)
