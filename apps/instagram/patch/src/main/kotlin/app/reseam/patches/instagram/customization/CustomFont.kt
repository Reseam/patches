// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.instagram.customization

import app.reseam.fonts.settings.FontStrings
import app.reseam.patch.settings.section
import app.reseam.patches.fonts.customFontFor
import app.reseam.patches.fonts.fontSettings
import app.reseam.patches.instagram.core.INSTAGRAM
import app.reseam.patches.instagram.core.instagramSettings

val customFont = customFontFor(INSTAGRAM, instagramSettings, section(FontStrings.sectionText, *fontSettings))
