// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.internal

import app.reseam.patch.ExtMethod
import app.reseam.patch.points
import app.reseam.patch.ExtClass
import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.before
import app.reseam.patch.classTarget
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.Opcode
import app.reseam.patch.dex.opcode
import app.reseam.patch.method
import app.reseam.patch.methodTarget
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.klass
import app.reseam.patch.replace
import app.reseam.patches.youtube.core.MAIN_ACTIVITY
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.youTubeSettings

private const val DRAWABLE = "android.graphics.drawable.Drawable"
private const val DRAWABLE_DESCRIPTOR = "Landroid/graphics/drawable/Drawable;"
private const val FRAME_LAYOUT = "android.widget.FrameLayout"
private const val TOOLBAR = "android.support.v7.widget.Toolbar"
private const val TOOLBAR_INTERFACE =
    "Lapp/reseam/youtube/navigation/NavigationBar\$AppCompatToolbarPatchInterface;"

/**
 * Keeps the extension's view state in step with YouTube's pivot bar.  The extension deliberately
 * owns the tab enum mapping and the back-button race workaround; patches only consume the stable
 * state API below.
 */
val navigationBarHook = patch {
    compatibleWith(YOUTUBE)
    dependsOn(youTubeSettings)

    execute {
        val actionBarSearchResultsLayout = requireNotNull(
            resources.id("layout", "action_bar_search_results_view_mic"),
        ) { "layout/action_bar_search_results_view_mic is missing" }.toLong()
        val toolbarContainerId = requireNotNull(resources.id("id", "toolbar_container")) {
            "id/toolbar_container is missing"
        }.toLong()

        val actionBarSearchResults = method("actionBarSearchResults") {
            flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
            returns(Type.View)
            literals(actionBarSearchResultsLayout)
            calls { name("setLayoutDirection"); params(Type.Int); returns(Type.Void) }
        }
        actionBarSearchResults.point("searchBarResultsViewLoaded") {
            invokeVirtual {
                name("setLayoutDirection")
                params(Type.Int)
                returns(Type.Void)
            }
        }.captureArgumentAs("searchBar", 0, Type.View).before {
            call(NavigationBar.searchBarResultsViewLoaded, capture("searchBar"))
        }

        val toolbarLayout = method("toolbarLayout") {
            flags(AccessFlags.PROTECTED or AccessFlags.CONSTRUCTOR)
            literals(toolbarContainerId)
            returns(Type.Void)
            opcode(Opcode.CHECK_CAST)
        }
        toolbarLayout.point("toolbarLoaded") {
            checkCast("Lcom/google/android/apps/youtube/app/ui/actionbar/MainCollapsingToolbarLayout;")
        }.captureAs("toolbar", FRAME_LAYOUT).after {
            call(NavigationBar.setToolbar, capture("toolbar"))
        }

        val pivotBarInitialized = method("pivotBarInitialized") {
            inClass(pivotBarClass)
            flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
            params(Type.Boolean)
            returns(Type.Void)
            strings("FEvideo_picker")
        }

        val enumOwner = navigationEnum.owner
        val pivotBarButtonsClass = klass("com.google.android.libraries.youtube.rendering.ui.pivotbar.PivotBar")
        val drawableCreator = method("pivotBarDrawableCreator") {
            inClass(pivotBarButtonsClass)
            flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
            returns(Type.View)
            param(0, DRAWABLE)
        }
        val styledCreator = method("pivotBarStyledCreator") {
            inClass(pivotBarButtonsClass)
            flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
            returns(Type.View)
            paramCount(4)
            param(1, Type.Boolean)
            param(2, Type.Int)
        }

        pivotBarInitialized.points("navigation enums") {
            invokeStatic { owner(enumOwner); returns(enumOwner) }
        }.forEach {
            next { resultOf(enumOwner) }.captureAs("enum").after {
                call(NavigationBar.setLastAppNavigationEnum, capture("enum"))
            }
        }
        listOf(
            drawableCreator to NavigationBar.navigationTabLoaded,
            styledCreator to NavigationBar.navigationImageResourceTabLoaded,
        ).forEach { (creator, hook) ->
            pivotBarInitialized.points("navigation tabs") { calls(creator) }.forEach {
                next { resultOf(Type.View) }.captureAs("tab").after { call(hook, capture("tab")) }
            }
        }

        navigationTabSelected
            .point("navigationTabSelected") { invokeVirtual { name("setSelected"); params(Type.Boolean); returns(Type.Void) } }
            .captureArgumentAs("view", 0, Type.View)
            .captureArgumentAs("selected", 1, Type.Boolean)
            .after {
                call(NavigationBar.navigationTabSelected, capture("view"), capture("selected"))
            }

        navigationMainActivityOnBackPressed
            .point("navigationBackPressed") { opcode(Opcode.RETURN_VOID) }
            .before { call(NavigationBar.onBackPressed, thisObject.cast(Type.Activity)) }

        // Resolve the framework method before adding the bridge; afterward the bridge has the
        // same public/final/zero-argument Drawable shape and would be an ambiguous query match.
        val actualToolbarMethod = toolbarBackButton.method
        val actualToolbarTarget = methodTarget("resolvedToolbarBackButton") { actualToolbarMethod }
        toolbarClass.classDef.addInterface(TOOLBAR_INTERFACE)
        val helper = appHelper(toolbarClass.classDef, "patch_getNavigationIcon", "()$DRAWABLE_DESCRIPTOR")
        helper.replace { returnValue(thisObject.call(actualToolbarTarget)) }
    }
}

object NavigationBar : ExtClass("app.reseam.youtube.navigation.NavigationBar") {
    val searchBarResultsViewLoaded = static("searchBarResultsViewLoaded", Type.View)
    val setToolbar = static("setToolbar", FRAME_LAYOUT)
    val setLastAppNavigationEnum = static("setLastAppNavigationEnum", Type.Object)
    val navigationTabLoaded = static("navigationTabLoaded", Type.View)
    val navigationImageResourceTabLoaded = static("navigationImageResourceTabLoaded", Type.View)
    val getNavigationButton = static(
        "getNavigationButton",
        Type.View,
        returns = "app.reseam.youtube.navigation.NavigationBar\$NavigationButton",
    )
    val navigationTabSelected = static("navigationTabSelected", Type.View, Type.Boolean)
    val onBackPressed = static("onBackPressed", Type.Activity)
}

private val pivotBarConstructor = method("pivotBarConstructor") {
    flags(AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR)
    strings("com.google.android.apps.youtube.app.endpoint.flags")
}

private val pivotBarClass = classTarget("pivotBarClass") {
    requireNotNull(bytecode.findClass(pivotBarConstructor.owner)) { "The pivot bar class is missing" }
}

private val navigationEnum = method("navigationEnum") {
    flags(AccessFlags.STATIC or AccessFlags.CONSTRUCTOR)
    returns(Type.Void)
    strings("PIVOT_HOME")
}

private val navigationTabSelected = method("navigationTabSelected") {
    inClass(klass("com.google.android.libraries.youtube.rendering.ui.pivotbar.PivotBar"))
    flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
    params(Type.Int, Type.Boolean)
    returns(Type.Void)
    calls { name("setSelected"); params(Type.Boolean); returns(Type.Void) }
}

private val navigationMainActivityOnBackPressed = method("navigationMainActivityOnBackPressed") {
    inClass(klass(MAIN_ACTIVITY))
    name("onBackPressed")
    flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
    params()
    returns(Type.Void)
}

private val toolbarClass = klass(TOOLBAR)

private val toolbarBackButton = method("toolbarBackButton") {
    inClass(toolbarClass)
    flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
    params()
    returns(DRAWABLE)
}

/** Observe the mapped view once, including image-resource tabs which delegate here. */
fun hookNavigationTabCreated(hook: ExtMethod) {
    NavigationBar.navigationTabLoaded.target.after {
        call(hook, call(NavigationBar.getNavigationButton, param(0)), param(0))
    }
}
