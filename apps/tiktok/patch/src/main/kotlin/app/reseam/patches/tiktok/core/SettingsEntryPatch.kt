// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.tiktok.core

import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.before
import app.reseam.patch.classTarget
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.Opcode
import app.reseam.patch.dex.literal
import app.reseam.patch.dex.ref
import app.reseam.patch.fieldOfType
import app.reseam.patch.fieldTarget
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.tiktok.entry.bindings.ProfileGear

private val signedOutProfile = klass("com.ss.android.ugc.aweme.unlogin.UnloginSignUpFragment")

val settingsGear =
    patch("Reseam settings") {
        description(
            "Adds a gear next to the menu button on the Profile tab that opens Reseam settings."
        )
        compatibleWith(TIKTOK)
        dependsOn(tiktokSettings)

        execute {
            val gear = resources.id("raw", "icon_gear")?.toInt() ?: error("raw/icon_gear missing")
            val menuButton =
                signedOutMenuId.instruction.literal
                    ?: error("No menu button id before the settings route")

            signedOutProfile.method("onViewCreated").after {
                val button =
                    param(0)
                        .callVirtual(
                            "android.view.View",
                            "findViewById",
                            "(I)Landroid/view/View;",
                            int(menuButton.toInt()),
                        )
                call(ProfileGear.attach, button, int(gear))
            }
            ProfileGear.actionKey.implement {
                returnValue(param(0).cast(navAction.descriptor).field(navActionKey))
            }
            ProfileGear.actionView.implement {
                whenInstanceOf(param(0), viewAction.descriptor) {
                    returnValue(param(0).cast(viewAction.descriptor).field(viewActionView))
                }
                returnNull()
            }
            ProfileGear.viewAction.implement {
                val action = newInstance(viewAction.descriptor)
                action.cast(navAction.descriptor).set(navActionKey, param(0))
                action.set(viewActionView, param(1))
                action.set(viewActionKeepsParams, bool(true))
                returnValue(action)
            }
            ProfileGear.isDarkMode.implement {
                returnValue(call(isDarkMode, param(0)))
            }
            setNavActions.before {
                val actions = param(0)
                call(
                    ProfileGear.addToNavActions,
                    thisObject,
                    actions.field(navActionList(0)),
                    actions.field(navActionList(1)),
                    int(gear),
                )
            }
        }
    }

private val isDarkMode = method("isDarkMode") {
    stringsStartingWith("Do not use application get them Settings")
}

// The signed-out profile's ≡ opens Settings and privacy; its id is the one compared just before
// that route.
private val signedOutMenuId =
    signedOutProfile
        .method("onClick")
        .point { string("//setting") }
        .previous { opcode(Opcode.CONST) }

private val navBar = klass("com.bytedance.tux.navigation.TuxNavBar")

private val setNavActions = navBar.method("setNavActions")

private val navActions =
    classTarget("navActions") {
        bytecode.findClass(setNavActions.method.parameterTypes.single())
            ?: error("The nav bar's actions class is missing")
    }

// Which list is the start and which the end does not matter: ≡ is found by its key in either.
private fun navActionList(index: Int) =
    fieldTarget("navActionList$index") {
        val lists = navActions.classDef.instanceFields.filter { it.fieldType == Type.List }
        check(lists.size == 2) { "The nav bar's actions have ${lists.size} lists" }
        lists[index].ref
    }

// Where the nav bar turns a view action into its child; it takes the view action class.
private val viewActionChild =
    method("viewActionChild") {
        inClass(navBar)
        flags(AccessFlags.STATIC)
        paramCount(1)
        returns(Type.View)
    }

private val viewAction =
    classTarget("viewAction") {
        bytecode.findClass(viewActionChild.method.parameterTypes.single())
            ?: error("The nav bar's view action class is missing")
    }

private val navAction =
    classTarget("navAction") {
        viewAction.classDef.superclass?.let(bytecode::findClass)
            ?: error("The nav bar's action class is missing")
    }

private val navActionKey = navAction.fieldOfType(Type.Object)
private val viewActionView = viewAction.fieldOfType(Type.View)
private val viewActionKeepsParams = viewAction.fieldOfType(Type.Boolean)
