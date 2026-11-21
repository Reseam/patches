// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.tiktok.appearance

import app.reseam.fonts.settings.FontStrings
import app.reseam.patch.settings.section
import app.reseam.patches.fonts.customFontFor
import app.reseam.patches.fonts.fontSettings
import app.reseam.patches.tiktok.core.TIKTOK
import app.reseam.patches.tiktok.core.TikTokSettingsPages
import app.reseam.patches.tiktok.core.tiktokSettings

val customFont = customFontFor(TIKTOK, tiktokSettings, section(TikTokSettingsPages.SearchAndApp, FontStrings.sectionText, *fontSettings))
