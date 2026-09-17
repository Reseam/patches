// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.internal

import app.reseam.patch.ExtClass
import app.reseam.patch.ExtMethod
import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.before
import app.reseam.patch.classTarget
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.DexClass
import app.reseam.patch.dex.Opcode
import app.reseam.patch.dex.opcode
import app.reseam.patch.dex.parameterTypes
import app.reseam.patch.dex.ref
import app.reseam.patch.dex.returnType
import app.reseam.patch.fieldOfType
import app.reseam.patch.fieldTarget
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.methods
import app.reseam.patch.native.NewField
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.replace
import app.reseam.patches.youtube.core.YOUTUBE

private const val EXTENSION_CLASS = "app.reseam.youtube.video.VideoInformation"
private const val PLAYBACK_CONTROLLER = "Lapp/reseam/youtube/video/VideoInformation\$PlaybackController;"
private const val VIDEO_QUALITY_MENU_INTERFACE =
    "Lapp/reseam/youtube/video/VideoInformation\$VideoQualityMenuInterface;"
const val EXTENSION_VIDEO_QUALITY_INTERFACE =
    "Lapp/reseam/youtube/video/VideoInformation\$VideoQualityInterface;"
private const val VIDEO_QUALITY_ARRAY = "[Lapp/reseam/youtube/video/VideoInformation\$VideoQualityInterface;"

private val onCreateHooks = mutableListOf<ExtMethod>()
private val videoTimeHooks = mutableListOf<ExtMethod>()
private val videoSpeedChangedHooks = mutableListOf<ExtMethod>()
private val userSelectedPlaybackSpeedHooks = mutableListOf<ExtMethod>()

object VideoInformation : ExtClass(EXTENSION_CLASS) {
    val initialize = static("initialize", PLAYBACK_CONTROLLER)
    val initializeMdx = static("initializeMDX", PLAYBACK_CONTROLLER)
    val setVideoId = static("setVideoId", Type.String)
    val newPlayerResponseSignature = static(
        "newPlayerResponseSignature",
        Type.String,
        Type.String,
        Type.Boolean,
        returns = Type.String,
    )
    val setPlayerResponseVideoId = static("setPlayerResponseVideoId", Type.String, Type.Boolean)
    val setVideoLength = static("setVideoLength", Type.Long)
    val setVideoTime = static("setVideoTime", Type.Long)
    val videoSpeedChanged = static("videoSpeedChanged", Type.Float)
    val userSelectedPlaybackSpeed = static("userSelectedPlaybackSpeed", Type.Float)
    val fixVideoQualityResolution = static("fixVideoQualityResolution", Type.String, Type.Int, returns = Type.Int)
    val setVideoQuality = static(
        "setVideoQuality",
        VIDEO_QUALITY_ARRAY,
        VIDEO_QUALITY_MENU_INTERFACE,
        Type.Int,
        returns = Type.Int,
    )
    val overridePlaybackSpeed = static("overridePlaybackSpeed", Type.Float)
}

/** Register a callback after the player controller has been initialized. */
fun onCreateHook(hook: ExtMethod) {
    require(hook.isStatic && hook.proto == "($PLAYBACK_CONTROLLER)V") {
        "An onCreate hook must be static (PlaybackController) -> Unit: $hook"
    }
    onCreateHooks += hook
}

/** Register a callback for the regular player's periodic time update. */
fun videoTimeHook(hook: ExtMethod) {
    require(hook.isStatic && hook.proto == "(J)V") {
        "A video-time hook must be static (long) -> Unit: $hook"
    }
    videoTimeHooks += hook
}

/** Register a callback when YouTube changes playback speed outside a menu selection. */
fun videoSpeedChangedHook(hook: ExtMethod) {
    require(hook.isStatic && hook.proto == "(F)V") {
        "A video-speed hook must be static (float) -> Unit: $hook"
    }
    videoSpeedChangedHooks += hook
}

/** Register a callback when the user selects a playback speed. */
fun userSelectedPlaybackSpeedHook(hook: ExtMethod) {
    require(hook.isStatic && hook.proto == "(F)V") {
        "A user-selected-speed hook must be static (float) -> Unit: $hook"
    }
    userSelectedPlaybackSpeedHooks += hook
}

private val playerParameterBuilderVideoClass = classTarget("playerParameterBuilderVideoClass") {
    bytecode.findClass(playVideoCheckVideoStreamingDataResponseMethod.owner)
        ?: error("The player class is missing")
}

private val mdxPlayerDirectorClass = classTarget("mdxPlayerDirectorClass") {
    bytecode.findClass(mdxPlayerDirectorSetVideoStageMethod.owner)
        ?: error("The MDX director class is missing")
}

// Each pinned player class declares one constructor; the singleton check makes that assumption
// explicit while allowing any constructor signature to survive a harmless signature change.
private val playerConstructor = playerParameterBuilderVideoClass.methods("playerConstructor") {
    flags(AccessFlags.CONSTRUCTOR)
    returns(Type.Void)
    custom { !isStatic }
}.single { true }

private val mdxPlayerConstructor = mdxPlayerDirectorClass.methods("mdxPlayerConstructor") {
    flags(AccessFlags.CONSTRUCTOR)
    returns(Type.Void)
    custom { !isStatic }
}.single { true }

private val seekSourceEnum = klass("seekSource") { strings("SEEK_SOURCE_UNKNOWN") }

private val playerSeekRelativeMethod = method("playerRelativeSeek") {
    inClass(playerParameterBuilderVideoClass)
    params(Type.Long, seekSourceEnum.descriptor)
    returns(Type.Boolean)
    opcode(Opcode.ADD_LONG_2ADDR)
}

private val playerSeekMethod = playerSeekRelativeMethod.point("absoluteSeek") {
    invokeVirtual { owner(playerParameterBuilderVideoClass.descriptor); params(Type.Long, seekSourceEnum.descriptor) }
}.callee()

private val mdxSeekRelativeMethod = method("mdxRelativeSeek") {
    inClass(mdxPlayerDirectorClass)
    params(Type.Long, seekSourceEnum.descriptor)
    returns(Type.Boolean)
    opcode(Opcode.ADD_LONG_2ADDR)
}

// Both controllers implement the same player interface, including absolute seeking.
private val mdxSeekMethod = method("mdxSeek") {
    inClass(mdxPlayerDirectorClass)
    name(playerSeekMethod.name)
    params(Type.Long, seekSourceEnum.descriptor)
    returns(Type.Boolean)
}

private val playerSeekSource = fieldTarget("playerSeekSource") {
    seekSourceEnum.method("<clinit>").point {
        string("SEEK_SOURCE_UNKNOWN")
    }.next {
        opcode(Opcode.SPUT_OBJECT)
        field { type(seekSourceEnum.descriptor) }
    }.field().ref
}

/** The player status callback used by later patches. */
val playerStatus = method("playerStatus") {
    flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
    params(playerStatusEnumClass.descriptor)
    returns(Type.Void)
    opcode(Opcode.SGET_OBJECT)
    calls { owner("Lj$/time/Instant;"); name("plus") }
}

private val playbackSpeedFactory = method("playbackSpeedProvider") {
    strings("PLAYBACK_RATE_MENU_BOTTOM_SHEET_FRAGMENT")
}.point { string("PLAYBACK_RATE_MENU_BOTTOM_SHEET_FRAGMENT") }
    .next { invokeStatic { hasParam(Type.String) } }.callee("playbackSpeedFactory")

private val onPlaybackSpeedItemClickClass = classTarget("playbackSpeedMenu") {
    bytecode.findClass(playbackSpeedFactory.returnType) ?: error("The playback-speed menu class is missing")
}

private val onPlaybackSpeedItemClickMethod = method("onPlaybackSpeedItemClickMethod") {
    inClass(onPlaybackSpeedItemClickClass)
    name("onItemClick")
    flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
    paramCount(4)
    param(2, Type.Int)
    param(3, Type.Long)
    custom {
        parameterTypes[0].startsWith("L") && parameterTypes[1].startsWith("L")
    }
    returns(Type.Void)
}

private val legacySpeedSelectionPoint = onPlaybackSpeedItemClickMethod.point {
    opcode(Opcode.IGET)
    field { type(Type.Float) }
}

// Keep the selected-speed capture on the shared point so both the core hook and later consumers
// can emit from the same resolved value.
private val legacySpeedSelection = legacySpeedSelectionPoint.captureAs("selectedSpeed", Type.Float)

private val playbackSpeedNullCheck = onPlaybackSpeedItemClickMethod.point {
    opcode(Opcode.IF_EQZ)
}

/** The legacy speed-menu field holding the speed-container object. */
val setPlaybackSpeedContainerClassField = playbackSpeedNullCheck
    .previous { opcode(Opcode.IGET_OBJECT) }
    .field("setPlaybackSpeedContainerClassField")

/** The field on the speed-container object that owns the playback-speed setter. */
val setPlaybackSpeedClassField = legacySpeedSelectionPoint
    .next { opcode(Opcode.IGET_OBJECT) }
    .field("setPlaybackSpeedClassField")

/** The app method invoked by the legacy speed menu to apply a selected speed. */
val setPlaybackSpeedMethod = legacySpeedSelectionPoint
    .next { invokeVirtual { params(Type.Float); returns(Type.Void) } }
    .callee("setPlaybackSpeedMethod")

internal val videoQualityClass = classTarget("videoQuality") {
    bytecode.findClass(videoQualitySetterMethod.parameterTypes[0].removePrefix("["))
        ?: error("The video-quality model class is missing")
}

private val videoQualityConstructor = method("videoQualityConstructor") {
    inClass(videoQualityClass)
    flags(AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR)
    paramCount(5)
    param(0, Type.Int)
    param(2, Type.String)
    param(3, Type.Boolean)
    returns(Type.Void)
    custom { parameterTypes[1].startsWith("L") && parameterTypes[4].startsWith("L") }
}

private val speedChangedMethod = method("speedChangedMethod") {
    calls(setPlaybackSpeedMethod)
    paramCount(1)
    custom { returnType.startsWith("L") }
}

private val speedChangedValue = speedChangedMethod.point {
    calls(setPlaybackSpeedMethod)
}.captureArgumentAs("playbackSpeed", 1, Type.Float)

private val videoQualitySetterMethod = method("videoQualitySetterMethod") {
    flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
    paramCount(3)
    param(1, Type.Int)
    param(2, Type.Boolean)
    returns(Type.Void)
    strings("menu_item_video_quality")
    opcode(Opcode.IF_EQZ, Opcode.INVOKE_VIRTUAL, Opcode.MOVE_RESULT_OBJECT, Opcode.INVOKE_VIRTUAL, Opcode.IPUT_BOOLEAN)
    custom { parameterTypes[0].startsWith("[L") }
}

private val videoQualitySetterClass = classTarget("videoQualitySetterClass") {
    bytecode.findClass(videoQualitySetterMethod.owner) ?: error("The video-quality menu class is missing")
}

// The structural query is the source's one setter-state method; the singleton check validates that the
// class has not gained a second method with the same field choreography.
private val getSetVideoQualityMethod = videoQualitySetterClass.methods("getSetVideoQualityMethod") {
    paramCount(1)
    returns(Type.Void)
    opcode(Opcode.IGET_OBJECT, Opcode.IPUT_OBJECT)
    custom { parameterTypes.singleOrNull()?.startsWith("L") == true }
}.single { true }

private val qualityListenerField = getSetVideoQualityMethod.point {
    opcode(Opcode.IGET_OBJECT)
}.field("qualityListenerField")

private val qualityMenuField = getSetVideoQualityMethod.point {
    opcode(Opcode.IPUT_OBJECT)
}.field("qualityMenuField")

private val qualityNameField = videoQualityClass.fieldOfType(Type.String)

private val qualityResolutionField = videoQualityClass.fieldOfType(Type.Int)

/** The quality constructor fields consumed by the extension. */
private val videoQualityMethod = videoQualityConstructor

private val playerStatusEnumMethod = method("playerStatusEnumMethod") {
    flags(AccessFlags.STATIC or AccessFlags.CONSTRUCTOR)
    strings("INTERSTITIAL_REQUESTED")
}

private val playerStatusEnumClass = classTarget("playerStatusEnumClass") {
    bytecode.findClass(playerStatusEnumMethod.owner) ?: error("The player-status enum class is missing")
}

private val playVideoCheckVideoStreamingDataResponseMethod = method("playVideoCheckVideoStreamingDataResponseMethod") {
    strings("playVideo called on player response with no videoStreamingData.")
}

private val mdxPlayerDirectorSetVideoStageMethod = method("mdxPlayerDirectorSetVideoStageMethod") {
    strings("MdxDirector setVideoStage ad should be null when videoStage is not an Ad state ")
}

val videoInformationHook = patch {
    compatibleWith(YOUTUBE)
    dependsOn(playerResponseHook, videoPlaybackStateHook)

    execute {
        // These are registered before the ID hooks' afterDependents phase, so every later patch
        // sees the same order: VideoInformation first, then its consumers.
        hookPlayerResponseVideoId(VideoInformation.setPlayerResponseVideoId)
        hookBeforeVideoId(VideoInformation.newPlayerResponseSignature)

        val playerClass = playerParameterBuilderVideoClass.classDef
        val mdxClass = mdxPlayerDirectorClass.classDef
        addPlaybackController(playerClass, playerSeekMethod, playerSeekRelativeMethod, playerSeekSource)
        addPlaybackController(mdxClass, mdxSeekMethod, mdxSeekRelativeMethod, playerSeekSource)

        videoQualityClass.classDef.addInterfaceIfMissing(EXTENSION_VIDEO_QUALITY_INTERFACE)
        // These are app-side interface implementations; the extension only declares the interface.
        appHelper(videoQualityClass.classDef, "patch_getQualityName", "()Ljava/lang/String;").replace {
            returnValue(thisObject.field(qualityNameField))
        }
        appHelper(videoQualityClass.classDef, "patch_getResolution", "()I").replace {
            returnValue(thisObject.field(qualityResolutionField))
        }

        val qualityMenuClass = klass(qualityMenuField.type)
        val qualityMenuSetterMethod = qualityMenuClass.methods("qualityMenuSetterMethod") {
            paramCount(1)
            param(0, videoQualityClass.descriptor)
            returns(Type.Void)
        }.single { true }
        val qualityMenu = qualityMenuClass.classDef
        qualityMenu.addInterfaceIfMissing(VIDEO_QUALITY_MENU_INTERFACE)
        val qualityHelper = appHelper(
            qualityMenu,
            "patch_setQuality",
            "($EXTENSION_VIDEO_QUALITY_INTERFACE)V",
        )
        qualityHelper.replace {
            thisObject.call(qualityMenuSetterMethod, param(0).cast(videoQualityClass.descriptor))
            returnVoid()
        }

        videoQualityMethod.before {
            param(0).assign(call(VideoInformation.fixVideoQualityResolution, param(2), param(0)))
        }
        videoQualitySetterMethod.before {
            val menu = thisObject.field(qualityListenerField).field(qualityMenuField)
            param(1).assign(
                call(
                    VideoInformation.setVideoQuality,
                    param(0).cast(VIDEO_QUALITY_ARRAY),
                    menu.cast(VIDEO_QUALITY_MENU_INTERFACE),
                    param(1),
                ),
            )
        }

        val playbackSpeedClass = klass(playbackSpeedFactory.returnType)
        val playbackSpeedOverride = appHelper(
            playbackSpeedClass.classDef,
            "overridePlaybackSpeed",
            "(F)V",
        )
        playbackSpeedOverride.replace {
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

        playbackSpeedFactory.after { setStatic(playbackSpeedField, capture("result")) }

        // The engine runs afterDependents only for patches that have dependents. These are the
        // core callbacks, so emit them here; consumer callbacks are appended below when needed.
        playerConstructor.after {
            call(VideoInformation.initialize, thisObject)
        }
        mdxPlayerConstructor.after {
            call(VideoInformation.initializeMdx, thisObject)
        }
        playerTimeMethod.before { call(VideoInformation.setVideoLength, param(2)) }
        setPlaybackSpeedMethod.before { call(VideoInformation.videoSpeedChanged, param(0)) }
        legacySpeedSelection.after {
            call(VideoInformation.userSelectedPlaybackSpeed, capture("selectedSpeed"))
        }
        speedChangedValue.before {
            call(VideoInformation.userSelectedPlaybackSpeed, capture("playbackSpeed"))
        }
    }

    afterDependents {
        if (onCreateHooks.isNotEmpty()) {
            playerConstructor.after {
                onCreateHooks.forEach { call(it, thisObject) }
            }
        }

        if (videoTimeHooks.isNotEmpty()) {
            playerTimeMethod.before {
                videoTimeHooks.forEach { call(it, param(0)) }
            }
        }

        if (videoSpeedChangedHooks.isNotEmpty()) {
            setPlaybackSpeedMethod.before {
                videoSpeedChangedHooks.forEach { call(it, param(0)) }
            }
        }

        if (userSelectedPlaybackSpeedHooks.isNotEmpty()) {
            legacySpeedSelection.after {
                userSelectedPlaybackSpeedHooks.forEach { call(it, capture("selectedSpeed")) }
            }
            speedChangedValue.before {
                userSelectedPlaybackSpeedHooks.forEach { call(it, capture("playbackSpeed")) }
            }
        }

        onCreateHooks.clear()
        videoTimeHooks.clear()
        videoSpeedChangedHooks.clear()
        userSelectedPlaybackSpeedHooks.clear()
    }
}

private fun DexClass.addInterfaceIfMissing(descriptor: String) {
    if (descriptor !in interfaces) addInterface(descriptor)
}

private fun addPlaybackController(
    clazz: DexClass,
    seek: app.reseam.patch.MethodTarget,
    seekRelative: app.reseam.patch.MethodTarget,
    seekSource: app.reseam.patch.FieldTarget,
) {
    clazz.addInterfaceIfMissing(PLAYBACK_CONTROLLER)
    val seekHelper = appHelper(clazz, "patch_seekTo", "(J)Z")
    seekHelper.replace {
        returnValue(thisObject.call(seek, param(0), staticField(seekSource)))
    }
    val relativeHelper = appHelper(clazz, "patch_seekToRelative", "(J)V")
    relativeHelper.replace {
        thisObject.call(seekRelative, param(0), staticField(seekSource))
        returnVoid()
    }
}
