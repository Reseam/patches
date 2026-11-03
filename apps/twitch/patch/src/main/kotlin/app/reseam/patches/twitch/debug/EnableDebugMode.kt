// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.twitch.debug

import app.reseam.patch.MethodTarget
import app.reseam.patch.MethodsTarget
import app.reseam.patch.PatchRuntime
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.points
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.twitch.core.TWITCH_PACKAGE
import app.reseam.patches.twitch.core.twitchSettings
import app.reseam.patches.twitch.shared.MAIN_SETTINGS_FRAGMENT
import app.reseam.patches.twitch.shared.forceTrue
import app.reseam.patches.twitch.shared.isStaff
import app.reseam.twitch.settings.DeveloperSettings
import app.reseam.twitch.settings.TwitchStrings

// The settings screen reads the account from its fragment before it lays out the debug row
val settingsScreenDebugRows = methods("settingsScreenDebugRows") {
    calls(isStaff)
    accesses { owner(MAIN_SETTINGS_FRAGMENT) }
}

// The player shows its debug panel only to staff, once they turn it on in the debug menu
val videoDebugPanelChecks = methods("videoDebugPanelChecks") {
    calls(isStaff)
    strings("showVideoDebugPanel")
}

// Twitch's web pages in the app read their debug options from the staff flag
val portalDebugChecks = methods("portalDebugChecks") {
    calls(isStaff)
    strings("shouldShowDebugOptions")
}

// The portal views get the staff flag as their shouldShowDebugOptions, next to the config they preload
val portalConfigChecks = methods("portalConfigChecks") {
    calls(isStaff)
    strings("settings-only", "eppo-only")
}

private fun PatchRuntime.staffChecksIn(target: MethodsTarget, place: String): List<MethodTarget> {
    val checks = target.all
    if (checks.isEmpty()) log.warn("$place no longer checks the staff flag")
    return checks
}

// The id literal exists only once resources.id resolves it, so these targets are built at patch time
private fun PatchRuntime.staffChecksUsing(type: String, name: String): List<MethodTarget> {
    val id = resources.id(type, name)?.toLong()
    if (id == null) {
        log.warn("The app has no $type/$name")
        return emptyList()
    }
    return staffChecksIn(methods("$type/$name staff checks") {
        calls(isStaff)
        literals(id)
    }, "The code that uses $type/$name")
}

// The Ultralight row in app settings reads the same staff override as the player flag it sets
private fun PatchRuntime.ultralightChecks(): List<MethodTarget> {
    val settingRows = staffChecksUsing("string", "ultralight_enabled_setting_title")
    if (settingRows.isEmpty()) return emptyList()

    val settingRow = settingRows.singleOrNull()
    if (settingRow == null) {
        log.warn("${settingRows.size} methods build the Ultralight row, so its staff override is ambiguous")
        return emptyList()
    }

    val ultralightStaffCheck = settingRow.point("ultralightStaffCheck") { calls(isStaff) }
    val ultralightOverride = ultralightStaffCheck.next { invokeVirtual { returns("Ljava/lang/Boolean;") } }.callee()
    return staffChecksIn(methods("ultralightChecks") {
        calls(isStaff)
        calls(ultralightOverride)
    }, "The Ultralight setting")
}

@Suppress("unused")
val enableDebugMode = patch("Enable debug mode") {
    description("Shows Twitch's staff tools, such as the debug menu, the video debug panel, the Ultralight setting and the debug options of its web pages.")
    compatibleWith(TWITCH_PACKAGE)
    enabledByDefault(false)
    settings(twitchSettings, section(TwitchStrings.sectionDeveloper, DeveloperSettings.debugMode))

    execute {
        // Every place that shows or opens a staff tool checks the staff flag on its own
        val staffChecks = listOf(
            staffChecksUsing("string", "settings_debug_menu"),
            staffChecksUsing("menu", "debug_app_actions"),
            staffChecksUsing("id", "debug_settings_menu_item"),
            staffChecksIn(settingsScreenDebugRows, "The settings screen"),
            staffChecksIn(videoDebugPanelChecks, "The video player"),
            staffChecksIn(portalDebugChecks, "The web portal plugin"),
            staffChecksIn(portalConfigChecks, "The web portal config"),
            ultralightChecks(),
        ).flatten().distinctBy { check -> check.descriptor }

        gate(DeveloperSettings.debugMode) {
            val staffCalls = staffChecks.flatMap { check -> check.points("staffCheck") { calls(isStaff) }.all }
            staffCalls.forEach { staffCall -> forceTrue(staffCall) }
            log.info("Opened the staff tools at ${staffCalls.size} staff checks in ${staffChecks.size} methods")
        }
    }
}
