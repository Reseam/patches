// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.core


// The app's player type and video state are obfuscated enums, so both hooks take the erased type
// and match on the constant name, which survives obfuscation.
