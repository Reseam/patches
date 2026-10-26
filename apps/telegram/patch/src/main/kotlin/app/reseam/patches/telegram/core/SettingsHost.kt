// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.telegram.core

import app.reseam.patch.Type
import app.reseam.patch.before
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.point
import app.reseam.patch.reserveLocal
import app.reseam.patch.settings.settingsHost
import app.reseam.patches.bundle.reseamAbout
import app.reseam.patches.bundle.reseamBrand
import app.reseam.telegram.settings.bindings.TelegramSettingsEntry

private const val ARRAY_LIST = "java.util.ArrayList"
private const val R_STRING = "org.telegram.messenger.R\$string"

val telegramSettings = settingsHost("telegram") {
    compatibleWith(TELEGRAM)
    brand(reseamBrand)
    about(reseamAbout)

    install {
        val logo = Resources::class.java.getResourceAsStream("/reseam-logo.png")?.use { it.readBytes() }
            ?: error("reseam-logo.png missing from patch jar resources")
        files.write("assets/reseam-patches/logo.png", logo)

        // R8 renames TextCell and UItem, so the row is built here from the resolved classes.
        TelegramSettingsEntry.reseamItem.implement {
            val activity = param(0)
            val cell = newInstance(textCell.descriptor, "(Landroid/content/Context;)V", activity)
            cell.call(setTextAndIcon, string("Reseam"), TelegramSettingsEntry.logo(activity), bool(false))
            cell.callVirtual("android.view.View", "setOnClickListener", "(Landroid/view/View\$OnClickListener;)V", TelegramSettingsEntry.opener())
            returnValue(call(asCustomItem, int(0), cell))
        }

        val activity = settingsFillItems.reserveLocal("parentActivity", "android.app.Activity")
        settingsFillItems.before { local(activity).assign(param(0).call(getParentActivity)) }
        settingsFillItems.point { field { owner(R_STRING); name("SettingsAccount") } }
            .next { invokeVirtual { owner(ARRAY_LIST); name("add"); params(Type.Object) } }
            .captureArgumentAs("items", 0)
            .before {
                whenNotNull(local(activity)) {
                    capture("items").callVirtual(ARRAY_LIST, "add", "(Ljava/lang/Object;)Z", TelegramSettingsEntry.reseamItem(local(activity)))
                }
            }
    }
}

private object Resources

// The suggestion keys appear together only in the settings list builder, which R8 made static.
val settingsFillItems = method("settingsFillItems") {
    strings("PREMIUM_GRACE", "VALIDATE_PASSWORD")
    hasParam(Type.ArrayList)
    returns(Type.Void)
}

val baseFragment = klass("baseFragment") { strings("trying to set current account when fragment UI already created") }
val getParentActivity = baseFragment.method("getParentActivity") { params() }

val listItem = klass("listItem") { strings("UItemFactory was not setuped: ") }
val asCustomItem = method("asCustomItem") {
    inClass(listItem)
    params(Type.Int, "android.view.View")
}

val textCell = klass("textCell") { strings("30_30", "paintDivider") }

// setTextAndValueDrawable shares the signature; only the left icon is tinted.
val setTextAndIcon = method("setTextAndIcon") {
    inClass(textCell)
    params(Type.CharSequence, DRAWABLE, Type.Boolean)
    calls { owner("android.widget.ImageView"); name("setColorFilter") }
}
