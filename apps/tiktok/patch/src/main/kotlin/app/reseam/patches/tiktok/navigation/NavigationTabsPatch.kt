// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.tiktok.navigation

import app.reseam.patch.CodeScope
import app.reseam.patch.Type
import app.reseam.patch.ValueRef
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.ToggleSetting
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.tiktok.TikTokPatchStrings
import app.reseam.patches.tiktok.core.TIKTOK
import app.reseam.patches.tiktok.core.TikTokSettingsPages
import app.reseam.patches.tiktok.core.tiktokSettings
import app.reseam.tiktok.settings.InterfaceSettings
import app.reseam.tiktok.settings.TikTokStrings

private const val AWEME = "com.ss.android.ugc.aweme"
private const val MAIN_FRAGMENT = "com.bytedance.tiktok.homepage.mainfragment"
private const val GONE = 8

// Removing a bottom tab makes TikTok rebuild both bars from other layouts, so those only lose their
// button.
private val BOTTOM_TABS: Map<ToggleSetting, List<String>> =
    mapOf(
        InterfaceSettings.hideShopTab to listOf("ecommerce.mall.ShopBottomTabProtocol"),
        InterfaceSettings.hideFriendsTab to
            listOf("tab.FriendsTabProtocol", "friendstab.ui.tab.protocol.FriendsTabV3Protocol"),
        InterfaceSettings.hideCreateButton to
            listOf("homepage.ui.view.tab.bottom.publishtab.PublishTabProtocol"),
    )

val navigationTabs =
    patch(TikTokPatchStrings.navigationTabs) {
        description(TikTokPatchStrings.navigationTabsDescription)
        compatibleWith(TIKTOK)
        settings(
            tiktokSettings,
            section(
                TikTokSettingsPages.SearchAndApp,
                TikTokStrings.sectionNavigation,
                *BOTTOM_TABS.keys.toTypedArray(),
            ),
        )

        execute {
            // TikTok drops a top tab that its own check turns off or its key registry doesn't know,
            // as in regions without Shop.
            gate(InterfaceSettings.hideShopTab) {
                shopTopTabEnabled.alwaysReturn(false)
                topTabByKey.after { whenInstanceOf(result, SHOP_TOP_TAB) { returnNull() } }
            }
            BOTTOM_TABS.forEach { (toggle, tabs) ->
                gate(toggle) {
                    BOTTOM_TAB_NODES.forEach { node ->
                        bottomTabView(node).after {
                            val tab = thisObject.fieldOfType("$MAIN_FRAGMENT.BottomTabProtocol")
                            tabs.forEach {
                                whenInstanceOf(tab, "$AWEME.$it") { hide(result) }
                            }
                        }
                    }
                }
            }
        }
    }

private const val SHOP_TOP_TAB = "$AWEME.ecommerce.mall.ShopTopTabProtocol"

private val shopTopTabEnabled = klass(SHOP_TOP_TAB).method("enable") { returns(Type.Boolean) }

private val tabRegistry =
    klass("tabRegistry") { hasInstanceField("Lcom/ss/android/ugc/aweme/spi/HomeTabWrapperService;") }

private val topTabByKey =
    method("topTabByKey") {
        inClass(tabRegistry)
        params(Type.String)
        returns("$MAIN_FRAGMENT.TopTabProtocol")
    }

private fun CodeScope.hide(view: ValueRef) =
    view.callVirtual("android.view.View", "setVisibility", "(I)V", int(GONE))

// A tab with top tabs of its own, such as Home, is a group.
private val BOTTOM_TAB_NODES = listOf("BottomTabNode", "BottomTabNodeGroup")

private fun bottomTabView(node: String) =
    method("bottomTabView$node") {
        inClass(klass("$AWEME.hometab.base.$node"))
        paramCount(1)
        returns("android.view.View")
    }
