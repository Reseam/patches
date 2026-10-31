// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.discord.themes

import app.reseam.runtime.Export
import com.facebook.react.BaseReactPackage
import com.facebook.react.ReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.module.model.ReactModuleInfo
import com.facebook.react.module.model.ReactModuleInfoProvider

class ThemesPackage : BaseReactPackage() {
    override fun getModule(name: String, context: ReactApplicationContext): NativeModule? =
        if (name == THEMES_MODULE_NAME) ThemesModule(context) else null

    override fun getReactModuleInfoProvider(): ReactModuleInfoProvider {
        val info =
            ReactModuleInfo(
                THEMES_MODULE_NAME,
                ThemesModule::class.java.name,
                false,
                false,
                false,
                false,
            )
        return ReactModuleInfoProvider { mapOf(THEMES_MODULE_NAME to info) }
    }

    companion object {
        @Export
        @JvmStatic
        fun addTo(packages: MutableList<ReactPackage>) {
            packages.add(ThemesPackage())
        }
    }
}
