// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later


package app.reseam.patches.instagram.media.download

import app.reseam.patch.fingerprint
import app.reseam.patch.parameterTypes
import app.reseam.patch.returnType

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
