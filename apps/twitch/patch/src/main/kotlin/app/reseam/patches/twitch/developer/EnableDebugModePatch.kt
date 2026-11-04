// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.twitch.developer

import app.reseam.patch.MethodTarget
import app.reseam.patch.MethodsTarget
import app.reseam.patch.PatchRuntime
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.points
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.twitch.core.TWITCH
import app.reseam.patches.twitch.core.forceTrue
import app.reseam.patches.twitch.core.isStaff
import app.reseam.patches.twitch.core.twitchSettings
import app.reseam.twitch.settings.DeveloperSettings
import app.reseam.twitch.settings.TwitchStrings

// Twitch's R8 rules keep the names of its fragments.
private const val MAIN_SETTINGS_FRAGMENT = "tv.twitch.android.settings.main.MainSettingsFragmentV2"

val enableDebugMode = patch("Enable debug mode") {
    description("Shows Twitch's staff tools, such as the debug menu, the video debug panel and the debug options of its web pages.")
    compatibleWith(TWITCH)
    enabledByDefault(false)
    settings(twitchSettings, section(TwitchStrings.sectionDeveloper, DeveloperSettings.debugMode))

    execute {
        // Each staff tool checks the staff flag itself.
        val staffChecks = listOf(
            staffChecksUsing("string", "settings_debug_menu"),
            staffChecksUsing("menu", "debug_app_actions"),
            staffChecksUsing("id", "debug_settings_menu_item"),
            staffChecksIn(settingsScreenDebugRows, "The settings screen"),
            staffChecksIn(videoDebugPanelChecks, "The video player"),
            staffChecksIn(portalDebugChecks, "The web portal plugin"),
            staffChecksIn(portalConfigChecks, "The web portal config"),
        ).flatten().distinctBy { it.descriptor }

        gate(DeveloperSettings.debugMode) {
            val staffCalls = staffChecks.flatMap { check -> check.points("staffCheck") { calls(isStaff) }.all }
            staffCalls.forEach { forceTrue(it) }
            log.info("Opened the staff tools at ${staffCalls.size} staff checks in ${staffChecks.size} methods")
        }
    }
}

private fun staffChecksIn(target: MethodsTarget, place: String): List<MethodTarget> =
    target.all.also { require(it.isNotEmpty()) { "$place no longer checks the staff flag" } }

// Resource ids are known only at patch time.
private fun PatchRuntime.staffChecksUsing(type: String, name: String): List<MethodTarget> {
    val id = resources.id(type, name)?.toLong() ?: error("The app has no $type/$name")
    return staffChecksIn(methods("$type/$name staff checks") { calls(isStaff); literals(id) }, "The code that uses $type/$name")
}

private val settingsScreenDebugRows = methods("settingsScreenDebugRows") {
    calls(isStaff)
    accesses { owner(MAIN_SETTINGS_FRAGMENT) }
}

private val videoDebugPanelChecks = methods("videoDebugPanelChecks") {
    calls(isStaff)
    strings("showVideoDebugPanel")
}

private val portalDebugChecks = methods("portalDebugChecks") {
    calls(isStaff)
    strings("shouldShowDebugOptions")
}

private val portalConfigChecks = methods("portalConfigChecks") {
    calls(isStaff)
    strings("settings-only", "eppo-only")
}
