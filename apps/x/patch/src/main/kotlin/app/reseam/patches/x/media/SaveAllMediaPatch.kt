// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.x.media

import app.reseam.patch.appEntry
import app.reseam.patch.before
import app.reseam.patch.dex.ref
import app.reseam.patch.fieldTarget
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.x.core.X
import app.reseam.patches.x.core.xSettings
import app.reseam.x.download.bindings.GalleryDownloads
import app.reseam.x.settings.XSettings
import app.reseam.x.settings.XStrings

private const val LIST = "Ljava/util/List;"

val saveAllMedia = patch("Save all media") {
    description("Offers to save every photo, GIF and video in a post when you save one of them.")
    compatibleWith(X)
    dependsOn(saveMedia)
    settings(xSettings, section(XStrings.sectionMedia, XSettings.saveAllMedia))

    execute {
        appEntry { GalleryDownloads.init(application) }
        GalleryDownloads.saveItem.implement {
            param(0).cast(gallerySave.owner).call(gallerySave, param(1).cast(gallerySave.parameterTypes[0]))
            returnVoid()
        }
        gallerySave.before {
            val media = thisObject.field(galleryArgs).field(galleryMedia)
            whenTrue(GalleryDownloads.offerSaveAll(thisObject, param(0), media)) { returnVoid() }
        }
    }
}

private val gallery = klass("mediaGallery") { strings("offline_videos_download_fallback", "video_download") }

private val gallerySave = method("gallerySave") { inClass(gallery); strings("offline_videos_download_fallback", "video_download") }

private val mediaGalleryArgs = klass("mediaGalleryArgs") { strings("MediaGalleryArgs(media=") }

private val galleryArgs = fieldTarget("galleryArgs") {
    gallery.classDef.instanceFields.single { it.fieldType == mediaGalleryArgs.descriptor }.ref
}

private val galleryMedia = fieldTarget("galleryMedia") {
    mediaGalleryArgs.classDef.instanceFields.single { it.fieldType == LIST }.ref
}
