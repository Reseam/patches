// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.twitch.settings

import app.reseam.runtime.Export

// R8 renames Function0 in Twitch, so the patch adds the settings row's click type and its method to this class
@Suppress("unused")
@Export
class OpenReseamSettings
