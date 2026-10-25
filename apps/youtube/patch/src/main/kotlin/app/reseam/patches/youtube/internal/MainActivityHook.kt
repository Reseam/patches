// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.internal

import app.reseam.patch.before
import app.reseam.patch.patch
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.mainActivityOnCreate
import app.reseam.youtube.core.bindings.YouTubeContext

/** Gives extensions YouTube's main activity through YouTubeContext.mainActivity. */
val mainActivityHook = patch {
    compatibleWith(YOUTUBE)

    execute {
        mainActivityOnCreate.before { YouTubeContext.setMainActivity(thisObject) }
    }
}
