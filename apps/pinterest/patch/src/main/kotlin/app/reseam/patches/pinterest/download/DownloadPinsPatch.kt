// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.pinterest.download

import app.reseam.patch.MethodQuery
import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.alwaysReturn
import app.reseam.patch.appEntry
import app.reseam.patch.before
import app.reseam.patch.enumConstant
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.points
import app.reseam.patch.settings.section
import app.reseam.patches.pinterest.core.PINTEREST
import app.reseam.patches.pinterest.core.pinterestSettings
import app.reseam.patches.pinterest.refs.holdsField
import app.reseam.patches.pinterest.refs.pinRefs
import app.reseam.patches.pinterest.refs.pinValue
import app.reseam.patches.pinterest.refs.requestOriginalSize
import app.reseam.patches.pinterest.refs.requestedImageSizes
import app.reseam.pinterest.download.bindings.PinDownloads
import app.reseam.pinterest.settings.DownloadSettings
import app.reseam.pinterest.settings.PinterestStrings

private const val FUNCTION0 = "kotlin.jvm.functions.Function0"
private const val FUNCTION1 = "kotlin.jvm.functions.Function1"

val downloadPins = patch("Download pins") {
    description("Downloads pins as original images and videos.")
    compatibleWith(PINTEREST)
    dependsOn(pinRefs)
    settings(pinterestSettings, section(PinterestStrings.sectionDownloads, DownloadSettings.downloadFolder))

    execute {
        val pin = pinValue("id").parameterTypes.single()
        val downloadTitle = resources.id("string", "download")?.toInt() ?: error("string/download missing")
        appEntry { PinDownloads.init(application) }
        requestedImageSizes.after { requestOriginalSize() }

        // Pinterest leaves Download out for videos, story pins and pins whose owner turned it off.
        pinMenus.forEach { after { returnValue(PinDownloads.withDownload(result, staticField(downloadAction), staticField(copyLinkAction))) } }
        downloadRowTitles.forEach { captureAs("title", Type.Int).after { capture("title").assign(int(downloadTitle)) } }
        // On a closeup the row otherwise opens the share sheet, whose save only writes JPEGs.
        inlineDownloadEducation.alwaysReturn(false)

        val closeMenu = pinMenuVideoSaves.all.associate { save ->
            save.owner to save.point { invokeVirtual { params(); returns(Type.Void) } }.callee("closePinMenu")
        }
        (pinMenuImageSaves.all + pinMenuVideoSaves.all).ifEmpty { error("No pin menu downloads") }.forEach { save ->
            save.before {
                thisObject.call(closeMenu.getValue(save.owner))
                PinDownloads.save(thisObject.fieldOfType(pin))
                returnVoid()
            }
        }
        // Other events start a download from the pin's id alone; only those carrying the pin can save its video.
        pinDownloadEvents.all.filter { holdsField(it.parameterTypes.single(), pin) }.forEach { event ->
            event.before {
                PinDownloads.save(param(0).fieldOfType(pin))
                returnVoid()
            }
        }
    }
}

private val pinMenuAction = klass("pinMenuAction") { enumConstants("COPY_LINK", "DOWNLOAD", "ADD_TO_COLLAGE", "REPORT") }
private val downloadAction = pinMenuAction.enumConstant("DOWNLOAD")
private val copyLinkAction = pinMenuAction.enumConstant("COPY_LINK")

private val pinMenus = methods("pinMenus") {
    accesses { owner(downloadAction.owner); name(downloadAction.name) }
    returns(Type.List)
}

private val downloadRowTitles = methods("downloadRowTitles") { accesses { name("save_to_device") } }
    .points("downloadRowTitle") { field { name("save_to_device") } }

private val inlineDownloadEducation = method("inlineDownloadEducation") {
    strings("enabled_inline_education")
    param(0, "android.widget.LinearLayout")
    returns(Type.Boolean)
}

private val downloadSource = klass("downloadSource") { enumConstants("OVERFLOW_MENU", "CLOSEUP", "POST_PIN_CREATION_TOAST", "DEEPLINK") }

private val pinMenuImageSaves = methods("pinMenuImageSaves") {
    callsImageSaver()
    param(0, Type.Context)
    paramCount(2)
    returns(Type.Void)
}

private val pinMenuVideoSaves = methods("pinMenuVideoSaves") {
    accesses { owner(downloadSource.descriptor); name(downloadSource.enumConstant("OVERFLOW_MENU").name) }
    params(Type.Context)
    returns(Type.Void)
}

private val pinDownloadEvents = methods("pinDownloadEvents") {
    name("onEventMainThread")
    callsImageSaver()
}

private fun MethodQuery.callsImageSaver() =
    calls { params(Type.Context, Type.Activity, FUNCTION1, FUNCTION0); returns(Type.Void) }
