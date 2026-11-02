// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.instagram.core

import app.reseam.patch.CompatiblePackage
import app.reseam.patch.invoke

const val INSTAGRAM_PACKAGE = "com.instagram.android"

val INSTAGRAM: CompatiblePackage = INSTAGRAM_PACKAGE("453.0.0.57.79")

const val USER_SESSION = "com.instagram.common.session.UserSession"
const val MEDIA_OPTION = "com.instagram.feed.media.mediaoption.MediaOption\$Option"
const val FRAGMENT_ACTIVITY = "androidx.fragment.app.FragmentActivity"
const val REEL_ITEM = "com.instagram.model.reels.ReelItem"
const val EXTENDED_IMAGE_URL = "com.instagram.model.mediasize.ExtendedImageUrl"
const val VIDEO_VERSION_INTF = "com.instagram.api.schemas.VideoVersionIntf"
const val FRIENDSHIP_STATUS = "com.instagram.user.model.FriendshipStatus"
const val PANDO_FRIENDSHIP_STATUS = "com.instagram.user.model.ImmutablePandoFriendshipStatus"
