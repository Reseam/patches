// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later


package app.reseam.patches.instagram.media

import app.reseam.patches.instagram.core.MediaSettings
import app.reseam.patches.instagram.core.settingsPatch

import app.reseam.patch.compatibleWith
import app.reseam.patch.findClass
import app.reseam.patch.findMethod
import app.reseam.patch.patch
import app.reseam.patch.settings.SettingsSection

val maxResolutionPatch = patch(
    name = "Max resolution",
    description = "Always loads the highest resolution image available.",
    compatibleWith = listOf(compatibleWith("com.instagram.android")),
    settingsHost = settingsPatch,
    dependsOn = listOf(settingsPatch),
    settings = listOf(
        SettingsSection(
            title = "Media",
            settings = listOf(MediaSettings.MaxResolution),
        ),
    ),
) {
    execute { ctx ->
        val selectorClass = ctx.findClass(debug = "imageUrlSelectorClass") {
            strings("_8.jpg", "_6.jpg")
        }
        val selector = ctx.findMethod(debug = "imageUrlSelector") {
            inClass(selectorClass)
            returnType("Lcom/instagram/model/mediasize/ExtendedImageUrl;")
            hasParameter("Ljava/util/List;")
        }

        selector.prependWhen(MediaSettings.MaxResolution) {
            val list = parameterOfType("Ljava/util/List;")
            val lastIndex = list.size().minus(int(1))
            val selected = list.get(lastIndex).cast("Lcom/instagram/model/mediasize/ExtendedImageUrl;")
            returnObject(selected)
        }

        ctx.log.info("Installed setting-controlled image resolution selection")
    }
}
