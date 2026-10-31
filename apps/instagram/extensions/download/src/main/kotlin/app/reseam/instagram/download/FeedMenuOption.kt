// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("FeedMenuOption")

package app.reseam.instagram.download

import app.reseam.runtime.Export

@Export external fun option(row: Any): Any?

@Export fun setLabel(row: Any, label: String) {}
