// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.hidelayout

import app.reseam.bundle.Logger
import app.reseam.runtime.Export
import app.reseam.youtube.core.debug
import app.reseam.youtube.litho.Filter
import app.reseam.youtube.litho.FilterGroup.ByteArrayFilterGroup
import app.reseam.youtube.litho.FilterGroup.StringFilterGroup
import app.reseam.youtube.litho.FilterRule
import app.reseam.youtube.litho.LithoComponent
import app.reseam.youtube.settings.FeedSettings

@Export
class LayoutComponentsFilter : Filter() {
    private val filtering = LayoutFiltering()

    init {
        addIdentifierCallbacks(*filtering.groups.identifierGroups)
        addPathCallbacks(*filtering.groups.pathGroups)
    }

    override fun isFiltered(component: LithoComponent, group: StringFilterGroup): Boolean =
        if (group is FilterRule) group.accepts(component)
        else filtering.allowsFeedPath(component.path)

    companion object {
        private val mixContextExceptions = listOf("V.ED", "java.lang.ref.WeakReference")
        private val mixBufferExceptions =
            ByteArrayFilterGroup(null, "cell_description_body", "channel_profile")
        private val mixPlaylists = ByteArrayFilterGroup(null, "&list=")

        @Export
        @JvmStatic
        fun filterMixPlaylists(conversionContext: Any?, buffer: ByteArray?): Boolean {
            if (!FeedSettings.hideMixPlaylists.value || buffer == null) return false
            if (
                !mixPlaylists.check(buffer).isFiltered ||
                    mixBufferExceptions.check(buffer).isFiltered
            )
                return false
            val context = conversionContext.toString()
            val hide = mixContextExceptions.none { it in context }
            if (hide) Logger.debug { "LayoutComponentsFilter filtered mix playlist" }
            return hide
        }
    }
}
