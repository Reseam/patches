// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtubecommon

import app.reseam.patch.patch

private object ReseamMarkResources

const val RESEAM_MARK = "reseam_adaptive_foreground"

/** The Reseam mark as a drawable, shared by the launcher icon and the header logo. */
val reseamMark = patch {
    execute {
        val mark = ReseamMarkResources::class.java.getResourceAsStream("/reseam-branding/drawable/$RESEAM_MARK.xml")
            ?.use { it.readBytes() }
            ?: error("reseam-branding/drawable/$RESEAM_MARK.xml is missing from the patch jar")
        resources.addFile("drawable", RESEAM_MARK, "res/drawable/$RESEAM_MARK.xml", mark)
    }
}
