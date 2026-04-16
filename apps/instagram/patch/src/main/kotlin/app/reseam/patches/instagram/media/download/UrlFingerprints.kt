// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later


package app.reseam.patches.instagram.media.download

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
