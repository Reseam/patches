// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.internal

import app.reseam.patch.addMethod
import app.reseam.patch.after
import app.reseam.patch.before
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.returnType
import app.reseam.patch.fieldTarget
import app.reseam.patch.klass
import app.reseam.patch.patch
import app.reseam.patch.replace
import app.reseam.patch.types.NewField
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.youtube.video.bindings.VideoInformation

val videoInformationHook = patch {
    compatibleWith(YOUTUBE)
    dependsOn(playerResponseHook, videoPlaybackStateHook)

    execute {
        // These are registered before the ID hooks' afterDependents phase, so every later patch
        // sees the same order: VideoInformation first, then its consumers.
        hookBeforeVideoId(VideoInformation.newPlayerResponseSignature)

        addPlaybackController(playerParameterBuilderVideoClass, playerSeekMethod, playerSeekRelativeMethod, playerSeekSource)
        addPlaybackController(mdxPlayerDirectorClass, mdxSeekMethod, mdxSeekRelativeMethod, playerSeekSource)

        installVideoQualityHooks()

        val playbackSpeedClass = klass(playbackSpeedFactory.returnType)
        playbackSpeedClass.addMethod("overridePlaybackSpeed", "(F)V") {
            val container = thisObject.field(setPlaybackSpeedContainerClassField)
            whenNotNull(container) {
                val speedSetter = container.cast(setPlaybackSpeedClassField.owner).field(setPlaybackSpeedClassField)
                whenNotNull(speedSetter) {
                    speedSetter.call(setPlaybackSpeedMethod, param(0))
                }
            }
            returnVoid()
        }

        val playbackSpeedField = fieldTarget("playbackSpeedClassField") {
            VideoInformation.target.classDef.field("playbackSpeedClass")
                ?: VideoInformation.target.classDef.addField(
                    NewField(
                        name = "playbackSpeedClass",
                        fieldType = playbackSpeedFactory.returnType,
                        accessFlags = (AccessFlags.PUBLIC or AccessFlags.STATIC).toUInt(),
                        initialValue = null,
                    ),
                ).let { VideoInformation.target.classDef.field("playbackSpeedClass")!! }
        }
        VideoInformation.overridePlaybackSpeed.implement {
            val player = staticField(playbackSpeedField)
            whenNotNull(player) {
                player.callVirtual(
                    playbackSpeedFactory.returnType,
                    "overridePlaybackSpeed",
                    "(F)V",
                    param(0),
                )
            }
            returnVoid()
        }

        playbackSpeedFactory.after { setStatic(playbackSpeedField, result) }

        // The engine runs afterDependents only for patches that have dependents. These are the
        // core callbacks, so emit them here; consumer callbacks are appended below when needed.
        playerConstructor.after {
            VideoInformation.initialize(thisObject)
        }
        mdxPlayerConstructor.after {
            VideoInformation.initializeMDX(thisObject)
        }
        playerTimeMethod.before { VideoInformation.setVideoWindow(param(2), param(3)) }
        setPlaybackSpeedMethod.before { VideoInformation.videoSpeedChanged(param(0)) }
        legacySpeedSelection.after {
            VideoInformation.userSelectedPlaybackSpeed(capture("selectedSpeed"))
        }
        speedChangedValue.before {
            VideoInformation.userSelectedPlaybackSpeed(capture("playbackSpeed"))
        }
    }

    afterDependents {
        if (onCreateHooks.values.isNotEmpty()) {
            playerConstructor.after {
                onCreateHooks.values.forEach { call(it, thisObject) }
            }
        }

        if (videoTimeHooks.values.isNotEmpty()) {
            playerTimeMethod.before {
                videoTimeHooks.values.forEach { call(it, param(0)) }
            }
        }

        if (videoSpeedChangedHooks.values.isNotEmpty()) {
            setPlaybackSpeedMethod.before {
                videoSpeedChangedHooks.values.forEach { call(it, param(0)) }
            }
        }

        if (userSelectedPlaybackSpeedHooks.values.isNotEmpty()) {
            legacySpeedSelection.after {
                userSelectedPlaybackSpeedHooks.values.forEach { call(it, capture("selectedSpeed")) }
            }
            speedChangedValue.before {
                userSelectedPlaybackSpeedHooks.values.forEach { call(it, capture("playbackSpeed")) }
            }
        }
    }
}
