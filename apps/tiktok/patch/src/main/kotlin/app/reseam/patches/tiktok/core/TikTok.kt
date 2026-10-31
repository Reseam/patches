// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.tiktok.core

import app.reseam.patch.CompatiblePackage
import app.reseam.patch.invoke

const val TIKTOK_PACKAGE = "com.zhiliaoapp.musically"

// Pinned: TikTok renames its obfuscated members every release, so only versions whose targets were
// read are listed.
val TIKTOK: CompatiblePackage = TIKTOK_PACKAGE("47.1.4")

const val AWEME = "com.ss.android.ugc.aweme.feed.model.Aweme"
const val FEED_ITEM_LIST = "com.ss.android.ugc.aweme.feed.model.FeedItemList"
const val VIDEO_ITEM_PARAMS = "com.ss.android.ugc.aweme.feed.model.VideoItemParams"
const val FEED_AVATAR = "com.ss.android.ugc.aweme.feed.assem.avatar"
