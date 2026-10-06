// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.universal

import app.reseam.patch.ExtClass
import app.reseam.patch.Type
import app.reseam.patch.patch

private const val LOCATION = "android.location.Location"

private object MockLocation : ExtClass("app.reseam.universal.location.MockLocation") {
    val isFromMockProvider by static(LOCATION, returns = Type.Boolean)
    val isMock by static(LOCATION, returns = Type.Boolean)
}

val hideMockLocation = patch("Hide mock location") {
    description("Hides that the location comes from a mock location app.")

    execute {
        val redirected = bytecode.redirectInstanceCalls(MockLocation.isFromMockProvider, MockLocation.isMock)
        log.info("Redirected $redirected mock location checks")
    }
}
