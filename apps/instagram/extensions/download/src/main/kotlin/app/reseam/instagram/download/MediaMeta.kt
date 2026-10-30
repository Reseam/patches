// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("MediaMeta")

package app.reseam.instagram.download

import app.reseam.runtime.Export

@Export external fun username(media: Any?): String?

@Export external fun reelItemMedia(reelItem: Any): Any?

@Export external fun storyOwnerReelItem(owner: Any): Any?

@Export external fun storyOwnerContext(owner: Any): Any?
