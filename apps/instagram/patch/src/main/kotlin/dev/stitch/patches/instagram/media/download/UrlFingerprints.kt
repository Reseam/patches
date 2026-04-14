/*
 * Stitch — part of the Stitch Android patcher.
 * Copyright (C) 2026 Aunali321 <accounts@auna.li>
 *
 * This file is part of Stitch.
 *
 * Stitch is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Stitch is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package dev.stitch.patches.instagram.media.download

internal const val VIDEO_URL_IMPL_TYPE = "Lcom/instagram/model/mediasize/VideoUrlImpl;"
internal const val EXTENDED_IMAGE_URL_TYPE = "Lcom/instagram/model/mediasize/ExtendedImageUrl;"
internal const val IMAGE_URL_INTERFACE_TYPE = "Lcom/instagram/common/typedurl/ImageUrl;"
internal const val IMAGE_URL_BASE_TYPE = "Lcom/instagram/common/typedurl/ImageUrlBase;"

// VideoUrlImpl's <init> validates the URL parameter is non-null and aborts with
// this unique message if not. Used as the anchor for discovering which of the
// two String fields on VideoUrlImpl holds the URL (independent of obfuscation
// field naming).
internal const val VIDEO_URL_NULL_CHECK_STRING = "trying to created a VideoUrl object with null url"

// Non-obfuscated interface wrapping each video_versions list element. Exposes
// getUrl()Ljava/lang/String;. Used as the check-cast anchor when correlating
// which obfuscated dict-getter returns the video_versions list.
internal const val VIDEO_VERSION_INTF_TYPE = "Lcom/instagram/model/mediasize/VideoVersionIntf;"
