// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.discord.layout

import app.reseam.discord.bindings.Quests
import app.reseam.discord.settings.DiscordSettings
import app.reseam.discord.settings.DiscordStrings
import app.reseam.patch.function
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.discord.DiscordPatchStrings
import app.reseam.patches.discord.core.DISCORD
import app.reseam.patches.discord.core.discordSettings

val hideQuests = patch(DiscordPatchStrings.hideQuests) {
    description(DiscordPatchStrings.hideQuestsDescription)
    compatibleWith(DISCORD)
    settings(discordSettings, section(DiscordStrings.sectionLayout, DiscordSettings.hideQuests))

    execute {
        gate(DiscordSettings.hideQuests) {
            questDock.alwaysReturnNull()
            questActivityButton.alwaysReturnNull()
            questsSettingVisible.alwaysReturn(false)
            useIsMobileQuestDockRenderedBase.alwaysReturn(false)
            useMobileQuestDockHeight.alwaysReturn(0)
            profileActions.before { Quests.enterProfileActions() }
            profileActions.after { Quests.leaveProfileActions() }
            getIsEligibleForQuests.before {
                whenTrue(Quests.isRenderingProfileActions()) { returnFalse() }
            }
        }
    }
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
