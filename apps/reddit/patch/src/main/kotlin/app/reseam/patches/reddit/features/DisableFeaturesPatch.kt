// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.reddit.features

import app.reseam.patch.MethodTarget
import app.reseam.patch.Type
import app.reseam.patch.before
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.ToggleSetting
import app.reseam.patch.settings.section
import app.reseam.patches.reddit.core.FlagOverrides
import app.reseam.patches.reddit.core.REDDIT
import app.reseam.patches.reddit.core.RedditSettings
import app.reseam.patches.reddit.core.redditSettings

private class FlagGroup(val disable: List<String> = emptyList(), val enable: List<String> = emptyList())

private val FLAG_GROUPS: Map<ToggleSetting, FlagGroup> = mapOf(
    // Answers stays on while any one of the three rollout flags is on.
    RedditSettings.hideAnswers to FlagGroup(disable = listOf(
        "answers_rollout",
        "answers_geo_allow_list",
        "answers_locale_allow_list",
        "android_ai_search_tab",
        "android_ai_search_tab_optimistic_loading_ks",
        "android_pdp_answers",
        "android_answers_text_selection_entry_point",
        "android_answers_pdp_preview",
        "android_search_bar_ask_button",
    )),
    RedditSettings.hideNotificationPrompts to FlagGroup(disable = listOf(
        "android_post_pn_reenablement_prompt",
        "android_comment_pn_reenablement_prompt",
        "android_return_pn_prompt_triggers",
        "android_return_pn_prompt_post_trigger",
    )),
    RedditSettings.hideUpdateNudges to FlagGroup(disable = listOf(
        "android_nudge_immediate_app_update",
        "android_hint_nudge_app_update",
    )),
    RedditSettings.hideShareNudges to FlagGroup(disable = listOf(
        "android_post_create_share_nudge",
        "android_post_save_share_nudge",
        "android_nudge_to_repost_expansion",
    )),
    RedditSettings.hideComposerPrompts to FlagGroup(disable = listOf(
        "android_inspiration_prompt_community",
        "android_inspiration_prompt_no_community",
    )),
    RedditSettings.hidePremiumUpsells to FlagGroup(disable = listOf(
        "android_econ_premium_ads_overflow_menu",
        "android_pro_native_upsell",
    )),
    RedditSettings.disableAdAutoplay to FlagGroup(disable = listOf(
        "android_ads_pdp_3_video_autoplay",
        "android_ads_animated_convo_video_previews",
    )),
    RedditSettings.hideSearchAds to FlagGroup(disable = listOf("android_search_ads_ff")),
    // Inputs of the Home and Popular feed queries; the Hide feed units patch hides what still arrives.
    RedditSettings.skipFeedUnitRequests to FlagGroup(disable = listOf(
        "android_trending_feed_unit_ks",
        "android_storycluster_popular",
        "android_feed_video_carousel",
        "android_continuous_onboarding_in_feed",
    )),
    RedditSettings.disableVideoChaining to FlagGroup(disable = listOf("android_fbp_m1_vertical_chaining")),
    // The server-driven bottom bar reads dynamic_games_tab_android instead of the Games nav flag.
    RedditSettings.hideGames to FlagGroup(disable = listOf(
        "android_games_bottom_nav",
        "dynamic_games_tab_android",
        "android_games_feed",
        "android_community_drawer_game",
        "android_devvit_games_feed_prewarm",
    )),
    RedditSettings.hideAchievementToasts to FlagGroup(enable = listOf("android_achievements_hide_progress_toast")),
    RedditSettings.hideChatDiscovery to FlagGroup(enable = listOf("android_chat_discovery_hidden_ks")),
)

val disableFeatures = patch("Disable Reddit features") {
    description("Turns off Reddit Answers, prompts, nudges, upsells, search ads, feed unit requests, ad autoplay, Games and more through Reddit's own feature flags.")
    compatibleWith(REDDIT)
    settings(redditSettings, section("Features", *FLAG_GROUPS.keys.toTypedArray()))

    execute {
        FlagOverrides.spec.implement {
            returnValue(string(FLAG_GROUPS.entries.joinToString(";") { (setting, group) ->
                "${setting.key}|${setting.default}|${group.disable.joinToString(",")}|${group.enable.joinToString(",")}"
            }))
        }
        // A disabled flag reads as no variant, which Reddit treats as off.
        listOf(mainReader, startupReader).forEach { reader ->
            reader.variant.before { whenTrue(call(FlagOverrides.isDisabled, param(0))) { returnNull() } }
            listOf(reader.enabled, reader.enabledUnexposed).forEach { enabled ->
                enabled.before {
                    whenTrue(call(FlagOverrides.isDisabled, param(0))) { returnFalse() }
                    whenTrue(call(FlagOverrides.isForced, param(0))) { returnTrue() }
                }
            }
        }
    }
}

private class FlagReader(name: String, val enabled: MethodTarget) {
    val variant = method("$name.variant") { inClass(klass(enabled.owner)); params(Type.String, Type.Boolean); returns(Type.String) }
    val enabledUnexposed = method("$name.enabledUnexposed") { inClass(klass(enabled.owner)); params(Type.String); returns(Type.Boolean) }
}

private val experimentReader = method("exposeExperiment") {
    calls(klass("com.reddit.experiments.RedditExperimentReader\$exposeExperiment\$2\$1").method("<init>"))
    returns(Type.Void)
}

private val experimentEnabled = method("experimentEnabled") {
    inClass(klass(experimentReader.owner))
    params(Type.String, Type.Boolean)
    returns(Type.Boolean)
}

private val mainReader = FlagReader("mainFlagReader", method("mainFlagReader") {
    calls(experimentEnabled)
    params(Type.String, Type.Boolean, Type.Boolean, Type.Boolean)
    returns(Type.Boolean)
})

private val startupReader = FlagReader("startupFlagReader", method("startupFlagReader") {
    strings("control")
    params(Type.String, Type.Boolean, Type.Boolean, Type.Boolean)
    returns(Type.Boolean)
})
