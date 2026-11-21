// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.x.theme

import app.reseam.fonts.settings.FontStrings
import app.reseam.patch.settings.section
import app.reseam.patches.fonts.customFontFor
import app.reseam.patches.fonts.fontSettings
import app.reseam.patches.x.core.X
import app.reseam.patches.x.core.XSettingsPages
import app.reseam.patches.x.core.xSettings

val customFont = customFontFor(X, xSettings, section(XSettingsPages.Appearance, FontStrings.sectionText, *fontSettings))
