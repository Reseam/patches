// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.core

import app.reseam.patch.CompatiblePackage
import app.reseam.patch.Type
import app.reseam.patch.invoke
import app.reseam.patch.klass
import app.reseam.patch.method

const val YOUTUBE_PACKAGE = "com.google.android.youtube"

// Pinned: YouTube rewrites its player and layout code every release, and several targets here
// replace method bodies, which goes wrong silently on a version the targets were not read against.
val YOUTUBE: CompatiblePackage = YOUTUBE_PACKAGE("21.37.42")

const val MAIN_ACTIVITY = "com.google.android.apps.youtube.app.watchwhile.MainActivity"

/** Shared lifecycle target for extensions that need the main activity. */
val mainActivityOnCreate = klass(MAIN_ACTIVITY).method("onCreate") {
    params("android.os.Bundle")
    returns(Type.Void)
}
