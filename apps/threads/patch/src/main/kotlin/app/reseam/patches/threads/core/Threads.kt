// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.threads.core

import app.reseam.patch.CompatiblePackage
import app.reseam.patch.invoke

const val THREADS_PACKAGE = "com.instagram.barcelona"

val THREADS: CompatiblePackage = THREADS_PACKAGE("453.0.0.56.79")
