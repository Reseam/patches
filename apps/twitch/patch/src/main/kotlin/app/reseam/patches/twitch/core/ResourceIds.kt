// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.twitch.core

import app.reseam.patch.PatchRuntime

// Resource ids are known only at patch time.
fun PatchRuntime.resourceId(type: String, name: String): Long = resources.id(type, name)?.toLong() ?: error("The app has no $type/$name")
