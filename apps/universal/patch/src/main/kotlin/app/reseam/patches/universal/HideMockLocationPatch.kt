// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.universal

import app.reseam.patch.patch
import app.reseam.universal.location.bindings.MockLocation

private const val LOCATION = "android.location.Location"
val hideMockLocation = patch(UniversalPatchStrings.hideMockLocation) {
    description(UniversalPatchStrings.hideMockLocationDescription)

    execute {
        val redirected = bytecode.redirectInstanceCalls(MockLocation.isFromMockProvider, MockLocation.isMock)
        log.info("Redirected $redirected mock location checks")
    }
}
