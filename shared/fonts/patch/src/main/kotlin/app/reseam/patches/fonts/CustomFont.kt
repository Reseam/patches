// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.fonts

import app.reseam.fonts.bindings.Fonts
import app.reseam.fonts.settings.FontSettings
import app.reseam.patch.CompatiblePackage
import app.reseam.patch.ExtMethod
import app.reseam.patch.PatchRuntime
import app.reseam.patch.ReseamPatch
import app.reseam.patch.Type
import app.reseam.patch.addMethod
import app.reseam.patch.appEntry
import app.reseam.patch.before
import app.reseam.patch.classTarget
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.DexClass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.SettingsHost
import app.reseam.patch.settings.SettingsSection
import app.reseam.patch.settings.onAction
import app.reseam.patch.types.MethodRef

val fontSettings = arrayOf(FontSettings.font, FontSettings.fontFile)

/** Draws [app]'s text in the font chosen in its settings; [section] places [fontSettings] on [host]'s screen. */
fun customFontFor(app: CompatiblePackage, host: SettingsHost, section: SettingsSection): ReseamPatch =
    patch(FontPatchStrings.customFont) {
        description(FontPatchStrings.customFontDescription)
        compatibleWith(app)
        settings(host, section)

        execute {
            appEntry { Fonts.init(application) }
            onAction(FontSettings.fontFile) { Fonts.pickFont(activity) }

            // Compose and custom text renderers set the typeface on a paint; the platform's text views
            // set it on theirs internally, so text views are covered by their own setTypeface.
            val paints = (PLATFORM_PAINTS + appSubclasses(PAINT)).sumOf { redirect(it, "($TYPEFACE)$TYPEFACE", Fonts.paintTypeface) }
            val views = PLATFORM_TEXT_VIEWS.sumOf {
                redirect(it, "($TYPEFACE)V", Fonts.viewTypeface) + redirect(it, "(${TYPEFACE}I)V", Fonts.viewTypefaceStyle)
            }
            // A text view applies the typeface from its XML attributes in the platform's constructor, out of
            // reach of call redirects, but through setTypeface, which the app's own text views can override.
            val roots = bytecode.classesExtending(TEXT_VIEW).filter { !it.isInterface && bytecode.findClass(it.superclass!!) == null }
            roots.forEach(::mapTypeface)
            log.info("Mapped typefaces at $paints paint and $views text view call sites and in ${roots.size} text view classes")
        }
    }

private fun PatchRuntime.appSubclasses(type: String) = bytecode.classesExtending(type).map(DexClass::descriptor)

private fun PatchRuntime.redirect(owner: String, proto: String, to: ExtMethod) =
    bytecode.redirectCalls(MethodRef(owner, "setTypeface", proto), to)

private fun mapTypeface(view: DexClass) {
    val target = classTarget(view.descriptor) { view }
    if (view.method("setTypeface", SET_TYPEFACE) != null) {
        target.method("setTypeface") { params(TYPEFACE); returns(Type.Void) }.before { param(0).assign(Fonts.typeface(param(0))) }
    } else {
        // Not final: the app's subclasses may override it in turn.
        target.addMethod("setTypeface", SET_TYPEFACE, AccessFlags.PUBLIC) {
            thisObject.callSuper(view.superclass!!, "setTypeface", SET_TYPEFACE, Fonts.typeface(param(0)))
            returnVoid()
        }
    }
}

private const val TYPEFACE = "Landroid/graphics/Typeface;"
private const val SET_TYPEFACE = "($TYPEFACE)V"
private const val PAINT = "Landroid/graphics/Paint;"
private const val TEXT_VIEW = "Landroid/widget/TextView;"

private val PLATFORM_PAINTS = listOf(PAINT, "Landroid/text/TextPaint;")

private val PLATFORM_TEXT_VIEWS =
    listOf(
        "TextView", "EditText", "Button", "CompoundButton", "CheckBox", "RadioButton", "Switch", "ToggleButton",
        "CheckedTextView", "AutoCompleteTextView", "MultiAutoCompleteTextView", "Chronometer", "TextClock",
    ).map { "Landroid/widget/$it;" }
