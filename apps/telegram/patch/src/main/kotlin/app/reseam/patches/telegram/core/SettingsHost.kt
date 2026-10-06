// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.telegram.core

import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.appEntry
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.settings.settingsHost

private const val ARRAY_LIST_ADD = "(Ljava/lang/Object;)Z"

val telegramSettings = settingsHost("telegram") {
    compatibleWith(TELEGRAM)

    install {
        val logo = Resources::class.java.getResourceAsStream("/reseam-logo.png")?.use { it.readBytes() }
            ?: error("reseam-logo.png missing from patch jar resources")
        files.write("assets/reseam/logo.png", logo)

        appEntry {
            call(TelegramSettingsEntry.init, application)
        }

        // R8 renames TextCell and UItem, so the row is built here from the resolved classes.
        TelegramSettingsEntry.reseamItem.implement {
            val activity = param(0)
            val cell = newInstance(textCell.descriptor, "(Landroid/content/Context;)V", activity)
            cell.call(setTextAndIcon, string("Reseam"), call(TelegramSettingsEntry.logo, activity), bool(false))
            cell.callVirtual("android.view.View", "setOnClickListener", "(Landroid/view/View\$OnClickListener;)V", call(TelegramSettingsEntry.opener))
            returnValue(call(asCustomItem, int(0), cell))
        }

        settingsFillItems.after {
            val activity = param(0).call(getParentActivity)
            whenNotNull(activity) {
                paramOfType(Type.ArrayList).callVirtual("java.util.ArrayList", "add", ARRAY_LIST_ADD, call(TelegramSettingsEntry.reseamItem, activity))
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
