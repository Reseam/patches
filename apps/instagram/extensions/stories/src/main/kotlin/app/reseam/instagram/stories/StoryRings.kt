// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("StoryRings")

package app.reseam.instagram.stories

import app.reseam.instagram.settings.StorySettings
import app.reseam.runtime.Export

@Export
fun scaled(size: Float): Float = size * StorySettings.ringSize.value.toInt() / 100
