// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.twitch.navigation

import app.reseam.patch.CodeScope
import app.reseam.patch.FieldTarget
import app.reseam.patch.PatchRuntime
import app.reseam.patch.PointTarget
import app.reseam.patch.Type
import app.reseam.patch.before
import app.reseam.patch.enumConstant
import app.reseam.patch.field
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.section
import app.reseam.patch.settings.valueOf
import app.reseam.patches.twitch.core.TWITCH
import app.reseam.patches.twitch.core.twitchSettings
import app.reseam.twitch.navigation.bindings.StartupTab
import app.reseam.twitch.settings.NavigationSettings
import app.reseam.twitch.settings.TwitchStrings

private const val DESTINATION_ORDINAL = "destinationOrdinal"

val startupTab = patch("Startup tab") {
    description("Starts Twitch on Following, Clips, Browse or Profile instead of Live.")
    compatibleWith(TWITCH)
    settings(twitchSettings, section(TwitchStrings.sectionNavigation, NavigationSettings.startupTab))

    execute {
        initialHomePage.before {
            whenTrue(opens("following")) { returnValue(staticField(followingPage)) }
            whenTrue(opens("live")) { returnValue(feedPage(liveLocation)) }
            whenTrue(opens("clips")) { returnValue(feedPage(clipsLocation)) }
        }

        val profileTab = resources.id("id", "viewer_bottom_nav_profile")?.toInt()
            ?: error("id/viewer_bottom_nav_profile missing")

        initialBottomTab().before {
            whenTrue(opens("profile")) {
                capture("initialTab").assign(int(profileTab))
            }
        }

        viewerLandingCreate.before {
            whenNull(param(0)) {
                whenTrue(opens("browse")) {
                    StartupTab.openBrowse(thisObject, string(DESTINATION_ORDINAL), staticField(defaultDestination), staticField(browseDestination))
                }
            }
        }
    }
}

private fun CodeScope.opens(tab: String) =
    valueOf(NavigationSettings.startupTab).callVirtual("java.lang.String", "equals", "(Ljava/lang/Object;)Z", string(tab))

private fun CodeScope.feedPage(location: FieldTarget) =
    newInstance(feedLocationPage.owner, feedLocationPage.proto, staticField(location))

private val initialHomePage = method("initialHomePage") {
    strings("default_home_page")
    returns("Ltv/twitch/android/models/feed/DiscoveryFeedPage;")
}

private val followingPage = klass($$"tv.twitch.android.models.feed.DiscoveryFeedPage$FollowingPage").field("INSTANCE")
private val liveLocation = klass($$"tv.twitch.android.models.feed.FeedLocation$Live").field("INSTANCE")
private val clipsLocation = klass($$"tv.twitch.android.models.feed.FeedLocation$Clips").field("INSTANCE")
private val feedLocationPage = klass($$"tv.twitch.android.models.feed.DiscoveryFeedPage$FeedLocationPage")
    .method("<init>") { params("Ltv/twitch/android/models/feed/FeedLocation;") }

private fun PatchRuntime.initialBottomTab(): PointTarget {
    val viewerMenu = resources.id("menu", "viewer_bottom_navigation_items")?.toLong()
        ?: error("menu/viewer_bottom_navigation_items missing")
    val homeTab = resources.id("id", "viewer_bottom_nav_home")?.toLong()
        ?: error("id/viewer_bottom_nav_home missing")

    return method("bottomNavigationStates") { literals(viewerMenu, homeTab) }
        .point("viewerMenu") { literal(viewerMenu) }
        .next { invokeDirect { name("<init>"); hasParam(Type.Map) } }
        .captureArgumentAs("initialTab", 2, Type.Int)
}

private val viewerLandingCreate = klass("tv.twitch.android.feature.viewer.landing.ViewerLandingActivity")
    .method("onCreate") { strings(DESTINATION_ORDINAL) }

private val destinations = klass("destinations") { enumConstants("Default", "Browse", "DiscoveryFeed") }
private val defaultDestination = destinations.enumConstant("Default")
private val browseDestination = destinations.enumConstant("Browse")
