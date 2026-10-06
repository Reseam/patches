// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtubemusic.core

import app.reseam.patch.CompatiblePackage
import app.reseam.patch.Type
import app.reseam.patch.invoke
import app.reseam.patch.klass
import app.reseam.patch.method

// Pinned: some targets replace method bodies, which fails silently on a version they were not read against.
val YOUTUBE_MUSIC: CompatiblePackage = "com.google.android.apps.youtube.music"("9.40.51")

const val MUSIC_ACTIVITY = "com.google.android.apps.youtube.music.activities.MusicActivity"

val musicActivity = klass(MUSIC_ACTIVITY)

val musicActivityOnCreate = musicActivity.method("onCreate") {
    params("android.os.Bundle")
    returns(Type.Void)
}
