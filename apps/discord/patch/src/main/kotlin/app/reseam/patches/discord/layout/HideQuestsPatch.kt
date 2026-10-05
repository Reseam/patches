// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.discord.layout

import app.reseam.patch.ExtJsModule
import app.reseam.patch.function
import app.reseam.patch.patch
import app.reseam.patch.settings.returnFalseWhen
import app.reseam.patch.settings.returnNullWhen
import app.reseam.patch.settings.section
import app.reseam.patch.settings.wrapWhen
import app.reseam.patches.discord.core.DISCORD
import app.reseam.patches.discord.core.DiscordSettings
import app.reseam.patches.discord.core.discordSettings

val hideQuests = patch("Hide quests") {
    description("Remove quest cards, profile buttons and the Quests settings entry.")
    compatibleWith(DISCORD)
    settings(discordSettings, section("Layout", DiscordSettings.hideQuests))

    execute {
        questDock.returnNullWhen(DiscordSettings.hideQuests)
        questActivityButton.returnNullWhen(DiscordSettings.hideQuests)
        questsSettingVisible.returnFalseWhen(DiscordSettings.hideQuests)
        useIsMobileQuestDockRenderedBase.returnFalseWhen(DiscordSettings.hideQuests)
        useMobileQuestDockHeight.wrapWhen(DiscordSettings.hideQuests, HideQuests.useMobileQuestDockHeight)
        profileActions.wrapWhen(DiscordSettings.hideQuests, HideQuests.profileActions)
        getIsEligibleForQuests.wrapWhen(DiscordSettings.hideQuests, HideQuests.getIsEligibleForQuests)
    }
}

private object HideQuests : ExtJsModule("discord-quests") {
    val useMobileQuestDockHeight = export("useMobileQuestDockHeight")
    val profileActions = export("profileActions")
    val getIsEligibleForQuests = export("getIsEligibleForQuests")
}

private val questDock = function {
    name("QuestDockWithVisibilityContext")
    strings("useMobileQuestDock", "useIsMobileQuestDockRenderedBase", "NO_FILL")
    paramCount(0)
}

private val questActivityButton = function {
    name("QuestActivityButton")
    strings("applicationId", "useStateFromStores", "useEffect")
    paramCount(1)
}

private val questsSettingVisible = function {
    name("usePredicate")
    strings("getIsEligibleForQuests")
    paramCount(0)
}

private val useIsMobileQuestDockRenderedBase = function {
    name("useIsMobileQuestDockRenderedBase")
    strings("QUEST_BAR_MOBILE", "getDeliveredAdCreativeId", "NO_FILL")
    paramCount(1)
}

private val useMobileQuestDockHeight = function {
    name("useMobileQuestDockHeight")
    strings("useQuestDockExternalOffset")
    paramCount(0)
}

private val profileActions = function {
    name("")
    strings("navigateToSettings", "getIsEligibleForQuests", "QuestsIcon")
    paramCount(1)
}

private val getIsEligibleForQuests = function {
    name("getIsEligibleForQuests")
    strings("isMetaQuest")
    paramCount(0)
}
