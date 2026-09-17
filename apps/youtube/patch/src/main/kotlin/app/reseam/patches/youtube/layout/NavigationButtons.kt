// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.layout

import app.reseam.patch.ExtClass
import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.Opcode
import app.reseam.patch.dex.parameterTypes
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettings
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.patches.youtube.internal.ClientContextEndpoint
import app.reseam.patches.youtube.internal.NavigationBar
import app.reseam.patches.youtube.internal.clientContextHook
import app.reseam.patches.youtube.internal.hookNavigationTabCreated
import app.reseam.patches.youtube.internal.navigationBarHook
import app.reseam.patches.youtube.internal.overrideBooleanFeature
import app.reseam.patches.youtube.internal.overrideClientContextOsName
import app.reseam.patches.youtube.internal.toolbarButtonIcon
import app.reseam.patches.youtube.internal.toolbarButtonIdentified

private const val PIVOT_BAR = "com.google.android.libraries.youtube.rendering.ui.pivotbar.PivotBar"
private const val TEXT_VIEW = "android.widget.TextView"
private const val CHAR_SEQUENCE = "java.lang.CharSequence"
private const val NAVIGATION_BUTTON =
    "app.reseam.youtube.navigation.NavigationBar\$NavigationButton"

private const val ANIMATED_NAVIGATION_TABS = 45680008L
private const val TRANSLUCENT_STATUS_BAR = 45400535L
private const val TRANSLUCENT_NAVIGATION_BUTTONS = 45630927L
private const val TRANSLUCENT_SYSTEM_BUTTONS = 45632194L

val navigationButtons = patch("Navigation bar") {
    description("Adds options to hide and change the bottom navigation bar and upper toolbar buttons.")
    compatibleWith(YOUTUBE)
    dependsOn(navigationBarHook, clientContextHook, youTubeSettings)
    settings(
        youTubeSettings,
        section(
            YouTubeSettingsPages.Appearance,
            "Navigation bar",
            YouTubeSettings.hideHomeButton,
            YouTubeSettings.hideShortsButton,
            YouTubeSettings.hideCreateButton,
            YouTubeSettings.hideSubscriptionsButton,
            YouTubeSettings.hideNotificationsButton,
            YouTubeSettings.hideLibraryButton,
            YouTubeSettings.switchCreateWithNotificationsButton,
            YouTubeSettings.hideNavigationButtonLabels,
            YouTubeSettings.narrowNavigationButtons,
            YouTubeSettings.navigationBarAnimations,
            YouTubeSettings.disableTranslucentStatusBar,
            YouTubeSettings.disableTranslucentNavigationBarLight,
            YouTubeSettings.disableTranslucentNavigationBarDark,
            YouTubeSettings.hideToolbarCreateButton,
            YouTubeSettings.hideToolbarNotificationButton,
            YouTubeSettings.hideToolbarSearchButton,
        ),
    )

    execute {
        overrideClientContextOsName(
            ClientContextEndpoint.GUIDE,
            YouTubeSettings.switchCreateWithNotificationsButton,
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
            call(NavigationButtons.hideNavigationButtonLabels, capture("label"))
        }

        toolbarButtonIdentified.after {
            call(
                NavigationButtons.hideCreateButton,
                capture("toolbarButton"),
                thisObject.field(toolbarButtonIcon),
            )
            call(
                NavigationButtons.hideNotificationButton,
                capture("toolbarButton"),
                thisObject.field(toolbarButtonIcon),
            )
            call(
                NavigationButtons.hideSearchButton,
                capture("toolbarButton"),
                thisObject.field(toolbarButtonIcon),
            )
        }

        overrideBooleanFeature(ANIMATED_NAVIGATION_TABS, NavigationButtons.useAnimatedNavigationButtons)
        overrideBooleanFeature(TRANSLUCENT_STATUS_BAR, NavigationButtons.useTranslucentNavigationStatusBar)
        overrideBooleanFeature(TRANSLUCENT_NAVIGATION_BUTTONS, NavigationButtons.useTranslucentNavigationButtons)
        overrideBooleanFeature(TRANSLUCENT_SYSTEM_BUTTONS, NavigationButtons.useTranslucentNavigationButtons)

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
                call(NavigationButtons.enableNarrowNavigationButton, capture("changedWidth")),
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
                call(NavigationButtons.enableNarrowNavigationButton, capture("styledWidth")),
            )
        }
    }
}

object NavigationButtons : ExtClass("app.reseam.youtube.navbuttons.NavigationButtons") {
    val navigationTabCreated = static(
        "navigationTabCreated",
        NAVIGATION_BUTTON,
        Type.View,
    )
    val hideNavigationButtonLabels = static("hideNavigationButtonLabels", TEXT_VIEW)
    val useAnimatedNavigationButtons = static(
        "useAnimatedNavigationButtons",
        Type.Boolean,
        returns = Type.Boolean,
    )
    val enableNarrowNavigationButton = static(
        "enableNarrowNavigationButton",
        Type.Boolean,
        returns = Type.Boolean,
    )
    val useTranslucentNavigationStatusBar = static(
        "useTranslucentNavigationStatusBar",
        Type.Boolean,
        returns = Type.Boolean,
    )
    val useTranslucentNavigationButtons = static(
        "useTranslucentNavigationButtons",
        Type.Boolean,
        returns = Type.Boolean,
    )
    val hideCreateButton = static("hideCreateButton", "java.lang.Enum", Type.View)
    val hideNotificationButton = static("hideNotificationButton", "java.lang.Enum", Type.View)
    val hideSearchButton = static("hideSearchButton", "java.lang.Enum", Type.View)
}
