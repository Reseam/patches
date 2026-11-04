// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.twitch.settings

import app.reseam.runtime.Export

// The patch adds the settings row's click type and its method, since R8 renamed Function0 in Twitch.
@Export
class OpenReseamSettings
