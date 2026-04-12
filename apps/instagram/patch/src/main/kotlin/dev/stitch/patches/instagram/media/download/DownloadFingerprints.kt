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

import dev.stitch.patch.fingerprint
import dev.stitch.patch.parameterTypes
import dev.stitch.patch.returnType

internal val feedMenuBuilderFingerprint = fingerprint {
    strings("instagram_feed_self_view_overflow_menu_insights_option_impression")
    returnType("Ljava/lang/Object;")
}

internal val feedMenuCreatorClassFingerprint = fingerprint {
    strings("MediaOptionsOverflowMenuCreator")
}

internal val feedClickHandlerFingerprint = fingerprint {
    strings("click_media_option", "MediaOptionsOverflowHelper")
    returnType("V")
    custom {
        parameterTypes.size == 1 && parameterTypes[0] == MEDIA_OPTION_TYPE
    }
}

internal val reelsClickHandlerFingerprint = fingerprint {
    strings("instagram_clips_overflow_menu_option_tap", "Unsupported click action for Clips Viewer Overflow menu.")
    returnType("V")
    custom {
        parameterTypes.size == 1 && parameterTypes[0] == MEDIA_OPTION_TYPE
    }
}

internal val storyActionSheetClassFingerprint = fingerprint {
    strings("archive_highlight_option", "copy_link_url", "delete_photo_title")
}
