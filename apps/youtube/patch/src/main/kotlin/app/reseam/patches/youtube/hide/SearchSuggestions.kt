// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.hide

import app.reseam.patch.FieldTarget
import app.reseam.patch.PatchRuntime
import app.reseam.patch.Type
import app.reseam.patch.addMethod
import app.reseam.patch.before
import app.reseam.patch.classTarget
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.Opcode
import app.reseam.patch.dex.opcode
import app.reseam.patch.dex.parameterTypes
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.point
import app.reseam.youtube.hidelayout.bindings.LayoutViews
import app.reseam.youtube.hidelayout.bindings.SearchSuggestionAccessor

private const val COLLECTION = "java.util.Collection"
private val SEARCH_SUGGESTION_ACCESSOR = SearchSuggestionAccessor.descriptor

internal fun PatchRuntime.hideSearchSuggestions() {
    val suggestionDivider = requireNotNull(resources.id("dimen", "suggestion_category_divider_height")) {
        "dimen/suggestion_category_divider_height is missing"
    }.toLong()
    val searchSuggestions = method("searchBoxTypingString") {
        flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
        paramCount(1)
        returns(Type.Void)
        literals(suggestionDivider)
        opcode(Opcode.IGET_OBJECT)
        calls { name("isEmpty"); params(); returns(Type.Boolean) }
    }
    val suggestionClass = classTarget("searchSuggestionClass") {
        requireNotNull(bytecode.findClass(searchSuggestionConstructor.owner)) {
            "The search suggestion class is missing"
        }
    }
    val suggestionEndpointMethod = method("searchSuggestionEndpoint") {
        inClass(suggestionClass)
        flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
        params()
        returns(Type.Boolean)
        calls { owner("Landroid/text/TextUtils;"); name("isEmpty") }
        opcode(Opcode.IGET_OBJECT)
    }
    val suggestionEndpointField = suggestionEndpointMethod
        .point("searchSuggestionEndpointField") {
            opcode(Opcode.IGET_OBJECT)
            field {
                owner(suggestionEndpointMethod.owner)
                type(Type.String)
            }
        }
        .field()
    val suggestionFields = searchSuggestionFields(searchSuggestions)
    suggestionClass.classDef.addInterface(SEARCH_SUGGESTION_ACCESSOR)
    suggestionClass.addMethod("patch_isSearchHistory", "()Z") {
        returnValue(
            LayoutViews.isSearchHistory(thisObject, thisObject.field(suggestionEndpointField)),
        )
    }
    val stateType = searchSuggestions.parameterTypes.single()
    val helper = klass(searchSuggestions.owner).addMethod("patch_setSearchSuggestions", "($stateType)V",
        AccessFlags.PRIVATE or AccessFlags.FINAL) {
        whenTrue(LayoutViews.hideYouMayLikeSection(param(0).field(suggestionFields.typedString))) {
            param(0).set(
                suggestionFields.collection,
                LayoutViews.filterSearchSuggestions(param(0).field(suggestionFields.collection)),
            )
        }
        returnVoid()
    }
    searchSuggestions.before { thisObject.call(helper, param(0)) }
}

private val searchSuggestionConstructor = method("searchSuggestionConstructor") {
    flags(AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR)
    returns(Type.Void)
    strings("… ")
    hasParam(Type.List)
}

private data class SearchSuggestionFields(val collection: FieldTarget, val typedString: FieldTarget)

private fun searchSuggestionFields(target: app.reseam.patch.MethodTarget): SearchSuggestionFields =
    SearchSuggestionFields(
        collection = target.point("searchSuggestionCollectionField") {
            opcode(Opcode.IGET_OBJECT)
            field { type(COLLECTION) }
        }.field(),
        typedString = target.point("searchSuggestionTypedStringField") {
            opcode(Opcode.IGET_OBJECT)
            field { type(Type.String) }
        }.field(),
    )
