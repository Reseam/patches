// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.video.quality

import app.reseam.patch.ExtClass
import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.before
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.Opcode
import app.reseam.patch.method
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.points
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettings
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.patches.youtube.internal.lithoFilter
import app.reseam.patches.youtube.internal.lithoRecyclerViewAttached
import app.reseam.patches.youtube.internal.registerLithoFilter
import app.reseam.patches.youtube.internal.videoInformationHook

object AdvancedVideoQualityMenu : ExtClass("app.reseam.youtube.quality.AdvancedVideoQualityMenu") {
    val onFlyoutMenuCreate = static("onFlyoutMenuCreate", Type.View)
    val addVideoQualityListMenuListener = static("addVideoQualityListMenuListener", Type.View)
    val forceAdvancedVideoQualityMenuCreation = static("forceAdvancedVideoQualityMenuCreation", Type.Boolean, returns = Type.Boolean)
}

private object AdvancedVideoQualityFilter : ExtClass("app.reseam.youtube.quality.AdvancedVideoQualityFilter")

val advancedVideoQualityMenu = patch("Advanced video quality menu") {
    description("Opens YouTube's detailed quality list from the quick quality menu.")
    compatibleWith(YOUTUBE)
    dependsOn(youTubeSettings, videoInformationHook, lithoFilter)
    settings(youTubeSettings, section(YouTubeSettingsPages.Video, "Video quality", YouTubeSettings.advancedVideoQualityMenu))

    execute {
        val titleId = resources.id("layout", "video_quality_bottom_sheet_list_fragment_title")?.toLong()
            ?: error("layout/video_quality_bottom_sheet_list_fragment_title is missing")
        val advancedDescriptionId = resources.id("string", "video_quality_quick_menu_advanced_menu_description")?.toLong()
            ?: error("string/video_quality_quick_menu_advanced_menu_description is missing")

        methods("video quality menu views") {
            params("android.view.LayoutInflater", "android.view.ViewGroup", "android.os.Bundle")
            returns(Type.View)
            literals(titleId)
        }.points("quality list") { checkCast("android.widget.ListView") }.forEach {
            captureAs("qualityList", Type.View).after {
                call(AdvancedVideoQualityMenu.addVideoQualityListMenuListener, capture("qualityList"))
            }
        }

        val options = method("video quality menu options") {
            flags(AccessFlags.STATIC)
            paramCount(3)
            param(0, Type.Context)
            literals(advancedDescriptionId)
            custom {
                returnType.startsWith("[L") && parameterTypes[1].startsWith("L") && parameterTypes[2].startsWith("L")
            }
        }
        options.point("advanced quality option") {
            opcode(Opcode.IGET_BOOLEAN)
            field { type(Type.Boolean) }
        }.captureAs("advancedOption", Type.Boolean).after {
            capture("advancedOption").assign(
                call(AdvancedVideoQualityMenu.forceAdvancedVideoQualityMenuCreation, capture("advancedOption")),
            )
        }

        lithoRecyclerViewAttached.before {
            call(AdvancedVideoQualityMenu.onFlyoutMenuCreate, param(1))
        }
        registerLithoFilter(AdvancedVideoQualityFilter)
    }
}
