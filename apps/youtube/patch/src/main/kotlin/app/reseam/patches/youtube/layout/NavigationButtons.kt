// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.layout

import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.Opcode
import app.reseam.patch.dex.parameterTypes
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.points
import app.reseam.patch.settings.section
import app.reseam.patch.settings.valueOf
import app.reseam.patch.skipWhen
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.patches.youtube.internal.ClientContextEndpoint
import app.reseam.patches.youtube.internal.clientContextHook
import app.reseam.patches.youtube.internal.hookNavigationTabCreated
import app.reseam.patches.youtube.internal.navigationBarHook
import app.reseam.patches.youtube.internal.overrideBooleanFeature
import app.reseam.patches.youtube.internal.overrideClientContextOsName
import app.reseam.patches.youtube.internal.toolbarButtonIcon
import app.reseam.patches.youtube.internal.toolbarButtonIdentified
import app.reseam.youtube.navbuttons.bindings.NavigationButtons
import app.reseam.youtube.navigation.bindings.NavigationBar
import app.reseam.youtube.settings.AppearanceSettings
import app.reseam.youtube.settings.YouTubeStrings

private const val PIVOT_BAR = "com.google.android.libraries.youtube.rendering.ui.pivotbar.PivotBar"
private const val TEXT_VIEW = "android.widget.TextView"
private const val NAVIGATION_BUTTON =
    "app.reseam.youtube.navigation.NavigationBar\$NavigationButton"

private const val ANIMATED_NAVIGATION_TABS = 45680008L
private const val TRANSLUCENT_STATUS_BAR = 45400535L
private const val TRANSLUCENT_NAVIGATION_BUTTONS = 45630927L
private const val TRANSLUCENT_SYSTEM_BUTTONS = 45632194L
private const val SCROLL_HIDES_HEADER = 45649805L
private const val SCROLL_HIDES_PIVOT_BAR = 45677535L
private const val RECYCLER_VIEW = "android.support.v7.widget.RecyclerView"

val navigationButtons = patch("Navigation bar") {
    description("Adds options to hide and change the bottom navigation bar and upper toolbar buttons.")
    compatibleWith(YOUTUBE)
    dependsOn(navigationBarHook, clientContextHook, youTubeSettings)
    settings(
        youTubeSettings,
        section(
            YouTubeSettingsPages.Appearance,
            YouTubeStrings.sectionNavigationBar,
            AppearanceSettings.hideHomeButton,
            AppearanceSettings.hideShortsButton,
            AppearanceSettings.hideCreateButton,
            AppearanceSettings.hideSubscriptionsButton,
            AppearanceSettings.hideNotificationsButton,
            AppearanceSettings.hideLibraryButton,
            AppearanceSettings.switchCreateWithNotificationsButton,
            AppearanceSettings.hideNavigationButtonLabels,
            AppearanceSettings.narrowNavigationButtons,
            AppearanceSettings.navigationBarAnimations,
            AppearanceSettings.keepNavigationBarVisible,
            AppearanceSettings.disableTranslucentStatusBar,
            AppearanceSettings.disableTranslucentNavigationBarLight,
            AppearanceSettings.disableTranslucentNavigationBarDark,
            AppearanceSettings.hideToolbarCreateButton,
            AppearanceSettings.hideToolbarNotificationButton,
            AppearanceSettings.hideToolbarSearchButton,
            AppearanceSettings.hideToolbarMessagesButton,
        ),
    )

    execute {
        overrideClientContextOsName(
            ClientContextEndpoint.GUIDE,
            AppearanceSettings.switchCreateWithNotificationsButton,
            "Android Automotive",
        )

        val pivotBarClass = klass(PIVOT_BAR)
        hookNavigationTabCreated(NavigationButtons.navigationTabCreated)

        val labelConstructor = method("navigationButtonLabelConstructor") {
            flags(AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR)
            params(PIVOT_BAR, TEXT_VIEW, CHAR_SEQUENCE)
            returns(Type.Void)
        }
        labelConstructor.point("navigation button label") {
            invokeVirtual {
                owner(TEXT_VIEW)
                name("setText")
                params(CHAR_SEQUENCE)
                returns(Type.Void)
            }
        }.captureArgumentAs("label", 0, TEXT_VIEW).after {
            NavigationButtons.hideNavigationButtonLabels(capture("label"))
        }

        toolbarButtonIdentified.after {
            NavigationButtons.hideCreateButton(capture("toolbarButton"), thisObject.field(toolbarButtonIcon))
            NavigationButtons.hideNotificationButton(capture("toolbarButton"), thisObject.field(toolbarButtonIcon))
            NavigationButtons.hideSearchButton(capture("toolbarButton"), thisObject.field(toolbarButtonIcon))
            NavigationButtons.hideMessagesButton(capture("toolbarButton"), thisObject.field(toolbarButtonIcon))
        }

        overrideBooleanFeature(ANIMATED_NAVIGATION_TABS, NavigationButtons.useAnimatedNavigationButtons)
        overrideBooleanFeature(TRANSLUCENT_STATUS_BAR, NavigationButtons.useTranslucentNavigationStatusBar)
        overrideBooleanFeature(TRANSLUCENT_NAVIGATION_BUTTONS, NavigationButtons.useTranslucentNavigationButtons)
        overrideBooleanFeature(TRANSLUCENT_SYSTEM_BUTTONS, NavigationButtons.useTranslucentNavigationButtons)

        // The feed scroll listeners hide the bar through (show, animate) calls: the pivot bar controller's,
        // and with experiment 45649805 on, the bottom bar container's as well. Older releases read that
        // experiment through a getter or use 45677535, and the oldest also hide the bar when scrolling stops.
        val scrollHidesHeaderGetters = methods("scrollHidesHeaderGetter") {
            params()
            returns(Type.Boolean)
            literals(SCROLL_HIDES_HEADER)
        }.all
        val feedScrolled = listOf(SCROLL_HIDES_HEADER, SCROLL_HIDES_PIVOT_BAR).map { flag ->
            methods("feedScrolled") { params(RECYCLER_VIEW, Type.Int, Type.Int); returns(Type.Void); literals(flag) }
        } + scrollHidesHeaderGetters.map { getter ->
            methods("feedScrolled") { params(RECYCLER_VIEW, Type.Int, Type.Int); returns(Type.Void); calls(getter) }
        }
        val listeners = feedScrolled.flatMap { it.all }.distinctBy { it.descriptor }
        val scrollStopped = listeners.map { it.owner }.distinct().flatMap { owner ->
            methods("feedScrollStopped") { inClass(klass(owner)); params(RECYCLER_VIEW, Type.Int); returns(Type.Void) }.all
        }
        (listeners + scrollStopped).flatMap { listener ->
            listener.points("navigationBarScrollVisibility") {
                invoke { params(Type.Boolean, Type.Boolean); returns(Type.Void) }
            }.all
        }.also { check(it.isNotEmpty()) { "No feed scroll listener toggles the navigation bar" } }
            .forEach { it.skipWhen { valueOf(AppearanceSettings.keepNavigationBarVisible) } }

        val pivotBarChanged = method("navigationPivotBarConfigurationChanged") {
            inClass(pivotBarClass)
            name("onConfigurationChanged")
            returns(Type.Void)
            paramCount(1)
        }
        pivotBarChanged.point("changed navigation width result") {
            invokeStatic { returns(Type.Boolean) }
            then { resultOf(Type.Boolean) }
        }.captureAs("changedWidth", Type.Boolean).after {
            capture("changedWidth").assign(
                NavigationButtons.enableNarrowNavigationButton(capture("changedWidth")),
            )
        }

        val pivotBarStyle = method("navigationPivotBarStyle") {
            inClass(pivotBarClass)
            returns(Type.Void)
            paramCount(1)
            opcode(Opcode.INVOKE_STATIC, Opcode.MOVE_RESULT, Opcode.XOR_INT_2ADDR)
            custom { parameterTypes.firstOrNull()?.startsWith("L") == true }
        }
        pivotBarStyle.point("styled navigation width result") {
            invokeStatic { returns(Type.Boolean) }
            then { resultOf(Type.Boolean) }
        }.captureAs("styledWidth", Type.Boolean).after {
            capture("styledWidth").assign(
                NavigationButtons.enableNarrowNavigationButton(capture("styledWidth")),
            )
        }
    }
}
