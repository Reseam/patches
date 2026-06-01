// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.telegram.privacy

import app.reseam.patch.FieldRef
import app.reseam.patch.compatibleWith
import app.reseam.patch.patch
import app.reseam.patch.settings.SettingsSection
import app.reseam.patch.settings.prependWhen
import app.reseam.patch.wrapField
import app.reseam.patches.telegram.core.TelegramSettings
import app.reseam.patches.telegram.core.settingsPatch

private const val ARCHIVE = "Lapp/reseam/telegram/antidelete/DeletedArchive;"
private const val WLP = "Landroid/view/WindowManager\$LayoutParams;"

val allowScreenshotsPatch = patch(
    name = "Allow screenshots in secret viewers",
    description = "Strips FLAG_SECURE from view-once / self-destruct media viewers so you can screenshot or screen-record.",
    compatibleWith = listOf(compatibleWith("org.telegram.messenger", "12.7.1")),
    settingsHost = settingsPatch,
    dependsOn = listOf(settingsPatch),
    settings = listOf(
        SettingsSection("Privacy", listOf(TelegramSettings.AllowScreenshots)),
    ),
) {
    extendWith("telegram-anti-delete.dex")
    execute { ctx ->
        val smv = ctx.bytecode.findClass("Lorg/telegram/ui/SecretMediaViewer;")
            ?: error("SecretMediaViewer class not found")
        val smvWlp = ctx.wrapField(
            smv.instanceFields.first { it.name == "windowLayoutParams" }.let {
                FieldRef(it.classDescriptor, it.name, it.fieldType)
            },
        )
        val openMedia = smv.methods.firstOrNull {
            it.info.methodName == "openMedia" &&
                it.info.proto.startsWith("(Lorg/telegram/messenger/MessageObject;")
        } ?: error("SecretMediaViewer.openMedia not found")
        openMedia.prependWhen(TelegramSettings.AllowScreenshots) {
            staticCall(ARCHIVE, "stripSecureFlag", "($WLP)V", thisObject().field(smvWlp))
        }
    }
}
