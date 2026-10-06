// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtubecommon

import app.reseam.patch.ExtClass
import app.reseam.patch.PatchRuntime
import app.reseam.patch.Type
import app.reseam.patch.methods
import app.reseam.patch.points
import app.reseam.patch.settings.ToggleSetting
import app.reseam.patch.settings.gate

private const val MENU = "android.view.Menu"
private const val MENU_ITEM = "android.view.MenuItem"

/** Hides every view or menu item the app looks up by the resource ID [name], while [setting] is on. */
fun PatchRuntime.hideById(name: String, setting: ToggleSetting) {
    val id = resources.id("id", name)?.toLong() ?: error("id/$name is missing")
    val users = methods("$name users") { literals(id) }
    val views = users.points("$name view lookup") {
        invokeVirtual { name("findViewById"); params(Type.Int); returns(Type.View) }
        argument(1) { literal(id) }
    }.all
    val items = users.points("$name menu item lookup") {
        invokeInterface { owner(MENU); name("findItem"); params(Type.Int) }
        argument(1) { literal(id) }
    }.all
    check(views.isNotEmpty() || items.isNotEmpty()) { "Nothing looks up id/$name" }
    gate(setting) {
        views.forEach { it.next { resultOf(Type.View) }.captureAs("view").after { call(Views.hide, capture("view")) } }
        items.forEach {
            it.next { resultOf(MENU_ITEM) }.captureAs("item").after {
                whenNotNull(capture("item")) {
                    capture("item").callInterface(MENU_ITEM, "setVisible", "(Z)Landroid/view/MenuItem;", bool(false))
                }
            }
        }
    }
}

private object Views : ExtClass("app.reseam.youtube.core.Views") {
    val hide by static(Type.View)
}
