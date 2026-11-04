// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.soundconnect.core

import app.reseam.patch.CompatiblePackage
import app.reseam.patch.invoke

const val SOUND_CONNECT_PACKAGE = "com.sony.songpal.mdr"

val SOUND_CONNECT: CompatiblePackage = SOUND_CONNECT_PACKAGE("13.3.0")
