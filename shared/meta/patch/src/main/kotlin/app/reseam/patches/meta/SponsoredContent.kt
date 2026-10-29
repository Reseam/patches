// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.meta

import app.reseam.patch.Type
import app.reseam.patch.method

val sponsoredContentInjection = method("sponsoredContentInjection") {
    strings("Is ad pod", "cross_surface_duplicate_ad")
    returns(Type.Boolean)
}
