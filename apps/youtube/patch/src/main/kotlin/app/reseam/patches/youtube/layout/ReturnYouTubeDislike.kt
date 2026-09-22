// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.layout

import app.reseam.patch.*
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.Opcode
import app.reseam.patch.dex.ref
import app.reseam.patch.dex.fieldRef
import app.reseam.patch.dex.literal
import app.reseam.patch.dex.opcode
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.*
import app.reseam.patches.youtube.internal.*

private const val CHAR_SEQUENCE = "java.lang.CharSequence"

val returnYouTubeDislike = patch("Return YouTube Dislike") {
    description("Shows dislike counts from Return YouTube Dislike and supports voting and Shorts.")
    compatibleWith(YOUTUBE)
    dependsOn(lithoFilter, videoInformationHook, playerTypeHook)
    settings(youTubeSettings, section(YouTubeSettingsPages.Video, "Return YouTube Dislike",
        YouTubeSettings.rydEnabled, YouTubeSettings.rydShorts, YouTubeSettings.rydDislikePercentage,
        YouTubeSettings.rydCompactLayout, YouTubeSettings.rydEstimatedLike, YouTubeSettings.rydToastOnConnectionError))

    execute {
        componentCreate.after {
            val span = call(NativeDislikeLabel.create, param(1).field(conversionContextPath))
            whenNotNull(span) {
                val button = capture("result").cast(nativeGesture.descriptor)
                val icon = button.field(nativeGesture.fieldOfType(componentCreate.returnType))
                val layout = icon.call(nativeCommonProperties).call(nativeLayoutProperties).cast(nativeHeight.owner)
                layout.set(nativeHeight, call(NativeDislikeLabel.iconHeight))
                layout.set(nativeMinHeight, call(NativeDislikeLabel.iconHeight))
                val heightMask = nativeHeightSetter.method.instructions.single {
                    it.opcode == Opcode.OR_INT_LIT8 || it.opcode == Opcode.OR_INT_LIT16
                }.literal!!.toInt()
                val minHeightMask = nativeMinHeightSetter.method.instructions.single {
                    it.opcode == Opcode.OR_INT_LIT8 || it.opcode == Opcode.OR_INT_LIT16
                }.literal!!.toInt()
                layout.set(nativeLayoutFlags, call(NativeDislikeLabel.withFlags,
                    layout.field(nativeLayoutFlags), int(heightMask or minHeightMask)))
                val text = newInstance(nativeText.descriptor)
                text.set(nativeTextValue, span)
                text.call(nativeCommonProperties).call(nativeWrapInView)
                val column = newInstance(nativeColumn.descriptor)
                column.set(nativeColumnAlignItems, int(3)) // Yoga CENTER, represented by the app's 1-based enum.
                column.set(nativeColumn.fieldOfType("java.util.List"),
                    call(NativeDislikeLabel.children, icon, text))
                button.set(nativeGesture.fieldOfType(componentCreate.returnType), column)
            }
        }
        nativeTextAccessibility.point {
            opcode(Opcode.IGET_OBJECT)
            field { type(CHAR_SEQUENCE) }
        }.captureAs("label", CHAR_SEQUENCE).after {
            capture("label").assign(call(NativeDislikeLabel.accessibilityText, capture("label")))
        }
        hookVideoId(Dislike.newVideoLoaded)
        hookPlayerResponseVideoId(Dislike.preloadVideoId)
        hookPlayerResponseVideoId(DislikeFilter.newPlayerResponseVideoId)
        registerLithoFilter(DislikeFilter)
        listOf("like/like" to 1, "like/dislike" to -1, "like/removelike" to 0).forEach { (endpoint, vote) ->
            method("vote $endpoint") { strings(endpoint); returns(Type.Void) }
                .before { call(Dislike.sendVote, int(vote)) }
        }

        val contextTypes = setOf(conversionContext.descriptor, conversionContext.classDef.info.superclass)
        val contextField = fieldTarget("text conversion context") {
            textComponent.classDef.instanceFields.single { it.fieldType in contextTypes }.ref
        }
        // Read only the resolved path, not the expensive context.toString(). Save it at entry:
        // current releases reuse the incoming this register.
        val context = textLookup.reserveLocal("dislikePath", "java.lang.StringBuilder")
        textLookup.before {
            local(context).assign(nullObject)
            val source = thisObject.field(contextField)
            whenNotNull(source) {
                local(context).assign(source.cast(conversionContext.descriptor).field(conversionContextPath))
            }
        }
        textLookup.point("cached text span") {
            opcode(Opcode.IPUT_OBJECT)
            field { type(CHAR_SEQUENCE) }
        }.captureAs("text", CHAR_SEQUENCE).before {
            capture("text").assign(call(Dislike.onLithoTextLoaded, local(context), capture("text")))
        }

        // Both TextComponent and TextViewComponent call this formatter. Following the call
        // covers the new rendering path without depending on the experiment flag or register offsets.
        val textFormatter = textLookup.point("text formatter") {
            invokeStatic { returns(CHAR_SEQUENCE); hasParam(contextField.ref.fieldType) }
        }.callee()
        check(textFormatter.parameterTypes.first() == contextField.ref.fieldType)
        textFormatter.after {
            whenNotNull(param(0)) {
                returnValue(call(Dislike.onLithoTextLoaded,
                    param(0).cast(conversionContext.descriptor).field(conversionContextPath), capture("result")))
            }
        }

        val rollingContext = rollingSetter.reserveLocal("rollingPath", "java.lang.StringBuilder")
        rollingSetter.before {
            local(rollingContext).assign(nullObject)
            whenNotNull(param(1)) {
                local(rollingContext).assign(param(1).cast(conversionContext.descriptor).field(conversionContextPath))
            }
        }
        rollingSetter.point("rolling count") {
            opcode(Opcode.IGET_OBJECT)
            field { type(Type.String) }
        }.captureAs("count", Type.String).after {
            capture("count").assign(call(Dislike.onRollingNumberLoaded, local(rollingContext), capture("count")))
        }

        // Widths are measured both statically and digit-by-digit; scope both to the font model.
        klass(rollingFontProperties.owner).methods("rolling text measurements") {
            params(Type.String)
            returns(Type.Float)
        }.forEach {
            after { returnValue(call(Dislike.onRollingNumberMeasured, param(0), capture("result"))) }
        }

        // Stable platform call + the view superclass pins this class without a whole-DEX scan.
        klass(rollingImageUpdate.owner).methods("rolling text updates") {
            calls { name("setText"); params(CHAR_SEQUENCE) }
        }.points("rolling text assignment") {
            invokeVirtual { name("setText"); params(CHAR_SEQUENCE) }
        }.forEach {
            captureArgumentAs("view", 0, "android.widget.TextView")
                .captureArgumentAs("text", 1, CHAR_SEQUENCE).before {
                    capture("text").assign(call(Dislike.updateRollingNumber, capture("view"), capture("text")))
                }
        }
    }
}

private val textComponent = klass("text component") { strings("TextComponent") }
private val nativeGesture = klass("native gesture component") { strings("ElementEventWithGesture") }
private val nativeColumn = klass("native Litho column") {
    strings("Column")
    extends(componentCreate.returnType)
}
private val nativeText = klass("native Litho text") {
    strings("Text")
    extends(componentCreate.returnType)
}
private val nativeTextAccessibility = method("native text accessibility") {
    inClass(nativeText)
    calls { owner(Type.View); name("getImportantForAccessibility") }
}
private val nativeTextValue = nativeTextAccessibility.point {
    opcode(Opcode.IGET_OBJECT)
    field { type(CHAR_SEQUENCE) }
}.field()
private val nativeCommonPropertiesField = fieldTarget("native component common properties") {
    klass(componentCreate.returnType).classDef.instanceFields.single { candidate ->
        bytecode.findClass(candidate.fieldType)?.instanceFields?.any {
            it.fieldType == "Landroid/graphics/drawable/Drawable;"
        } == true
    }.ref
}
private val nativeCommonProperties = method("native component properties getter") {
    inClass(klass(componentCreate.returnType))
    params()
    returns(nativeCommonPropertiesField.type)
}
private val nativeWrapInView = method("native component accessibility host") {
    inClass(klass(nativeCommonPropertiesField.type))
    params()
    returns(Type.Void)
    opcode(Opcode.IPUT_BOOLEAN)
}
// Follow Yoga's retained platform bridge back to the column's align-items property.
private val yogaLayout = method("native Litho Yoga layout") {
    calls { owner("com.facebook.yoga.YogaNative"); name("jni_YGNodeStyleSetAlignItemsJNI") }
    calls { owner("com.facebook.yoga.YogaNative"); name("jni_YGNodeStyleSetFlexDirectionJNI") }
}
private val yogaAlignItems = yogaLayout.point {
    invokeStatic { owner("com.facebook.yoga.YogaNative"); name("jni_YGNodeStyleSetAlignItemsJNI") }
}.previous { opcode(Opcode.IGET); field { type(Type.Int) } }.field()
private val nativeColumnAlignItems = method("native column layout") {
    inClass(nativeColumn)
    returns(yogaAlignItems.ref.definingClass)
}.point {
    opcode(Opcode.IPUT)
    field { owner(yogaAlignItems.ref.definingClass); name(yogaAlignItems.ref.name) }
}.previous { opcode(Opcode.IGET); field { owner(nativeColumn.descriptor); type(Type.Int) } }.field()
private val yogaMinHeight = method("Yoga minimum height adapter") {
    params(Type.Int)
    calls { owner("com.facebook.yoga.YogaNative"); name("jni_YGNodeStyleSetMinHeightJNI") }
}
private val yogaHeightBridge = method("Yoga height bridge") {
    inClass(klass("com.facebook.yoga.YogaNodeJNIBase"))
    calls { owner("com.facebook.yoga.YogaNative"); name("jni_YGNodeStyleSetHeightJNI") }
}
private val yogaHeight = method("Yoga height adapter") {
    inClass(klass(yogaMinHeight.owner))
    params(Type.Int)
    calls { name(yogaHeightBridge.name); params(*yogaHeightBridge.parameterTypes.toTypedArray()) }
}
private val nativeHeight = yogaLayout.point {
    invokeInterface { name(yogaHeight.name); params(Type.Int); returns(Type.Void) }
}.writer(1).field()
private val nativeMinHeight = yogaLayout.point {
    invokeInterface { name(yogaMinHeight.name); params(Type.Int); returns(Type.Void) }
}.writer(1).field()
private val nativeBuilder = klass("native component builder") { strings("unknown component") }
private fun sizeSetter(size: FieldTarget) = method("native size setter") {
    inClass(nativeBuilder)
    params(Type.Int)
    returns(Type.Void)
    custom { instructions.any { it.opcode == Opcode.IPUT && it.fieldRef == size.ref } }
}
private val nativeHeightSetter = sizeSetter(nativeHeight)
private val nativeMinHeightSetter = sizeSetter(nativeMinHeight)
private val nativeLayoutFlags = nativeHeightSetter.point { opcode(Opcode.IGET) }.field()
private val nativeLayoutProperties = nativeHeightSetter.point {
    invokeVirtual { owner(nativeCommonPropertiesField.type) }
}.callee()
private object NativeDislikeLabel : ExtClass("app.reseam.youtube.dislike.DislikeLabel") {
    val iconHeight = static("iconHeight", returns = Type.Int)
    val withFlags = static("withFlags", Type.Int, Type.Int, returns = Type.Int)
    val create = static("create", "java.lang.StringBuilder", returns = CHAR_SEQUENCE)
    val children = static("children", "java.lang.Object", "java.lang.Object", returns = "java.util.List")
    val accessibilityText = static("accessibilityText", CHAR_SEQUENCE, returns = CHAR_SEQUENCE)
}
private val textLookup = method("cached text component") {
    inClass(textComponent)
    strings("…")
    flags(AccessFlags.PROTECTED or AccessFlags.FINAL)
    paramCount(1)
}
private val rollingSetter = method("rolling number model builder") {
    stringsStartingWith("RollingNumberType required properties missing!")
    paramCount(7)
}
private val rollingFontProperties = method("rolling number font properties") {
    strings("RollingNumberFontProperties{paint=")
    name("toString")
    returns(Type.String)
}
private val rollingImageUpdate = method("rolling number image update") {
    params("android.graphics.Bitmap")
    returns(Type.Void)
    calls { owner("android.text.SpannableString"); name("setSpan") }
    custom {
        classDef.info.superclass == "Landroid/support/v7/widget/AppCompatTextView;" ||
            classDef.info.superclass == "Lcom/google/android/libraries/youtube/rendering/ui/spec/typography/YouTubeAppCompatTextView;"
    }
}

private object DislikeFilter : ExtClass("app.reseam.youtube.dislike.ReturnYouTubeDislikeFilter") {
    val newPlayerResponseVideoId = static("newPlayerResponseVideoId", Type.String, Type.Boolean)
}
private object Dislike : ExtClass("app.reseam.youtube.dislike.ReturnYouTubeDislikePatch") {
    val newVideoLoaded = static("newVideoLoaded", Type.String)
    val preloadVideoId = static("preloadVideoId", Type.String, Type.Boolean)
    val sendVote = static("sendVote", Type.Int)
    val onLithoTextLoaded = static("onLithoTextLoaded", "java.lang.StringBuilder", CHAR_SEQUENCE, returns = CHAR_SEQUENCE)
    val onRollingNumberLoaded = static("onRollingNumberLoaded", "java.lang.StringBuilder", Type.String, returns = Type.String)
    val onRollingNumberMeasured = static("onRollingNumberMeasured", Type.String, Type.Float, returns = Type.Float)
    val updateRollingNumber = static("updateRollingNumber", "android.widget.TextView", CHAR_SEQUENCE, returns = CHAR_SEQUENCE)
}
