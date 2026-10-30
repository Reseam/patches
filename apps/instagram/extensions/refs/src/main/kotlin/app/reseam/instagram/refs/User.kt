// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("User")

package app.reseam.instagram.refs

import app.reseam.runtime.Export

@Export external fun fromMedia(media: Any?): Any?

@Export external fun username(user: Any?): String?
