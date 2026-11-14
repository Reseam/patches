// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.x.ailabels

import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.appEntry
import app.reseam.patch.before
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.points
import app.reseam.patch.settings.section
import app.reseam.patches.ailabels.AiLabelsPatchStrings
import app.reseam.patches.ailabels.aiLabelSettings
import app.reseam.patches.ailabels.aiLabelsSupport
import app.reseam.patches.pangram.pangramSupport
import app.reseam.patches.x.core.COMPOSER
import app.reseam.patches.x.core.X
import app.reseam.patches.x.core.XColors
import app.reseam.patches.x.core.XSettingsPages
import app.reseam.patches.x.core.androidView
import app.reseam.patches.x.core.availablePost
import app.reseam.patches.x.core.composerAtEntry
import app.reseam.patches.x.core.mediaPhoto
import app.reseam.patches.x.core.postHeader
import app.reseam.patches.x.core.unitInstance
import app.reseam.patches.x.core.xApi
import app.reseam.patches.x.core.xPosts
import app.reseam.patches.x.core.xSettings
import app.reseam.x.ailabels.bindings.XAiLabels
import app.reseam.x.postheader.bindings.ImageLabelFactory
import app.reseam.x.postheader.bindings.ImageLabelUpdate
import app.reseam.x.postheader.bindings.TextLabelFactory
import app.reseam.x.postheader.bindings.TextLabelUpdate
import app.reseam.x.settings.XStrings

val showAiLabels = patch(AiLabelsPatchStrings.showAiLabels) {
    description(AiLabelsPatchStrings.showAiLabelsDescription)
    compatibleWith(X)
    dependsOn(xApi, xPosts)
    settings(xSettings, section(XSettingsPages.Home, XStrings.sectionAiLabels, *aiLabelSettings.toTypedArray()))

    execute {
        aiLabelsSupport()
        pangramSupport()
        implementPalette()
        appEntry { XAiLabels.prefetchTimelines() }

        TextLabelFactory.invoke.implement { returnValue(XAiLabels.textLabel(param(0).cast(Type.Context))) }
        TextLabelUpdate.invoke.implement {
            XAiLabels.bindText(param(0).cast(Type.View), thisObject.fieldOfType(Type.Object))
            returnValue(staticField(unitInstance))
        }
        ImageLabelFactory.invoke.implement { returnValue(XAiLabels.imageLabel(param(0).cast(Type.Context))) }
        ImageLabelUpdate.invoke.implement {
            XAiLabels.bindImage(param(0).cast(Type.View), thisObject.fieldOfType(Type.Object))
            returnValue(staticField(unitInstance))
        }

        // The text label sits in the header row just before the options button, so it lays out like X's own
        // header items. It is composed for every post, keeping the composition's shape fixed.
        val (optionsButton, postHeaders) = postHeader()
        postHeaders.all.forEach { header ->
            val composer = header.composerAtEntry()
            header.points { calls(optionsButton) }.forEach {
                before {
                    XAiLabels.applyPalette(call(XColors.current, local(composer)))
                    val update = newInstance(TextLabelUpdate.descriptor, "(${Type.Object})V", paramOfType(availablePost.descriptor))
                    androidView(newInstance(TextLabelFactory.descriptor), update, local(composer))
                }
            }
        }

        // The image label follows the photo in its box, so it draws on top, at the box's default top start
        // corner, away from X's own ALT and GIF badges at the bottom.
        val photoComposer = photoBox.composerAtEntry()
        photoBox.points { invokeStatic { hasParam(mediaPhoto.descriptor); hasParam(COMPOSER); returns(Type.Void) } }.forEach {
            after {
                val update = newInstance(ImageLabelUpdate.descriptor, "(${Type.Object})V", paramOfType(mediaPhoto.descriptor))
                androidView(newInstance(ImageLabelFactory.descriptor), update, local(photoComposer))
            }
        }
    }
}

// The composable that lays out one photo in a post: the photo, then X's badges over it.
private val photoBox = method("photoBox") {
    strings("LocalSharedAnimationScope not provided")
    param(0, mediaPhoto.descriptor)
}

private fun implementPalette() {
    XAiLabels.applyPalette.implement {
        val colors = param(0).cast(XColors.colors.descriptor)
        XAiLabels.usePalette(
            colors.field(XColors.backgroundSheets),
            colors.field(XColors.backgroundSecondary),
            colors.field(XColors.foregroundPrimary),
            colors.field(XColors.foregroundSecondary),
            colors.field(XColors.borderNormal),
            colors.field(XColors.foregroundBrand),
            colors.field(XColors.buttonPrimary),
            colors.field(XColors.foregroundInverted),
            colors.field(XColors.isLight),
        )
        returnVoid()
    }
}
