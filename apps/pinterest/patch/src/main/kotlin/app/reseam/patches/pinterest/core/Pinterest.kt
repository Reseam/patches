// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.pinterest.core

import app.reseam.patch.CompatiblePackage
import app.reseam.patch.invoke
import app.reseam.patch.klass
import app.reseam.patch.method

const val PINTEREST_PACKAGE = "com.pinterest"

val PINTEREST: CompatiblePackage = PINTEREST_PACKAGE("14.42.0")

const val PINTEREST_SIGNATURE = "b6a74dbcb894b0f73d8c485c72eb1247a8f027ca"

val mainActivityOnCreate = klass("com.pinterest.activity.PinterestActivity").method("onCreate")
