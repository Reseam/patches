// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.navigation

import android.app.Activity
import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import app.reseam.bundle.Logger
import app.reseam.bundle.identifier
import app.reseam.runtime.Export
import app.reseam.youtube.core.debug
import java.lang.ref.WeakReference
import java.util.WeakHashMap

object NavigationBar {
    @Export
    interface AppCompatToolbarPatchInterface {
        @Export fun patch_getNavigationIcon(): Drawable?
    }

    @Volatile private var searchBarResults = WeakReference<View>(null)
    @Volatile private var toolbar = WeakReference<AppCompatToolbarPatchInterface>(null)
    @Volatile private var lastNavigationEnum: String? = null
    private val viewToButton = WeakHashMap<View?, NavigationButton>()
    private val selectionLatch = NavigationSelectionLatch(NavigationBar::class.java)

    init {
        selectionLatch.reset()
    }

    val isSearchBarActive: Boolean
        get() = searchBarResults.get()?.isShown == true

    val isBackButtonVisible: Boolean
        get() = toolbar.get()?.patch_getNavigationIcon() != null

    /** The query of the search results on screen, however the search was started. */
    @Volatile
    var searchQuery = ""
        private set

    @Export
    @JvmStatic
    fun searchBarResultsViewLoaded(view: View) {
        searchBarResults = WeakReference(view)
        val query = view.findViewById<TextView>(view.context.identifier("search_query", "id")) ?: return
        searchQuery = query.text.toString()
        query.addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: Editable) {
                    searchQuery = s.toString()
                }
            }
        )
    }

    @Export
    @JvmStatic
    fun setToolbar(layout: FrameLayout?) {
        val found = findToolbar(layout)
        if (found == null) {
            Logger.debug { "Navigation toolbar was not found" }
            return
        }
        toolbar = WeakReference(found)
    }

    private fun findToolbar(view: View?): AppCompatToolbarPatchInterface? =
        when (view) {
            is AppCompatToolbarPatchInterface -> view
            is ViewGroup ->
                (0 until view.childCount).firstNotNullOfOrNull { findToolbar(view.getChildAt(it)) }
            else -> null
        }

    @Export
    @JvmStatic
    fun setLastAppNavigationEnum(navigationEnum: Any?) {
        if (navigationEnum !is Enum<*>) return
        lastNavigationEnum = navigationEnum.name
        Logger.debug { "Navigation tab created: $lastNavigationEnum" }
    }

    @Export
    @JvmStatic
    fun navigationTabLoaded(tabView: View?) {
        if (tabView == null) return
        val enumName = lastNavigationEnum
        val button = NavigationButton.fromAppName(enumName)
        if (button == null) {
            Logger.debug { "Navigation tab unmapped: $enumName" }
            return
        }
        viewToButton[tabView] = button
        Logger.debug { "Navigation tab mapped: $button" }
    }

    @Export
    @JvmStatic
    fun getNavigationButton(tabView: View?): NavigationButton? = viewToButton[tabView]

    @Export
    @JvmStatic
    fun navigationImageResourceTabLoaded(tabView: View?) {
        if (!NavigationButton.CREATE.matches(lastNavigationEnum)) {
            lastNavigationEnum = NavigationButton.LIBRARY.enumNames[0]
        }
        navigationTabLoaded(tabView)
    }

    @Export
    @JvmStatic
    fun navigationTabSelected(view: View?, selected: Boolean) {
        if (!selected) {
            Logger.debug { "Navigation tab deselected: $view" }
            return
        }
        val button = viewToButton[view]
        if (button == null) {
            Logger.debug { "Navigation tab selected but unmapped: $view" }
            NavigationButton.selected = null
            return
        }
        NavigationButton.selected = button
        Logger.debug { "Navigation tab selected: $button" }
        selectionLatch.release()
    }

    @Export
    @JvmStatic
    fun onBackPressed(activity: Activity) {
        selectionLatch.reset()
    }

    @Export
    enum class NavigationButton(vararg names: String) {
        HOME("PIVOT_HOME", "TAB_HOME_CAIRO"),
        SHORTS("TAB_SHORTS", "TAB_SHORTS_CAIRO"),
        CREATE("CREATION_TAB_LARGE", "CREATION_TAB_LARGE_CAIRO"),
        EXPLORE("TAB_EXPLORE"),
        SUBSCRIPTIONS("PIVOT_SUBSCRIPTIONS", "TAB_SUBSCRIPTIONS_CAIRO"),
        NOTIFICATIONS("TAB_ACTIVITY", "TAB_ACTIVITY_CAIRO"),
        LIBRARY(
            "YOU_LIBRARY_DUMMY_PLACEHOLDER_NAME",
            "ACCOUNT_CIRCLE",
            "ACCOUNT_CIRCLE_CAIRO",
            "INCOGNITO_CIRCLE",
            "INCOGNITO_CAIRO",
            "VIDEO_LIBRARY_WHITE",
            "PIVOT_LIBRARY",
        );

        internal val enumNames = names.toList()

        internal fun matches(name: String?): Boolean = name in enumNames

        companion object {
            private val byAppName =
                entries.flatMap { button -> button.enumNames.map { it to button } }.toMap()

            internal fun fromAppName(name: String?): NavigationButton? = byAppName[name]

            @Volatile internal var selected: NavigationButton? = null

            /** Last mapped tab, waiting up to 120 ms off the main thread for pending selection. */
            val selectedNavigationButton: NavigationButton?
                get() {
                    // Access through NavigationBar initializes the first selection latch.
                    NavigationBar.selectionLatch.awaitSelection()
                    return selected
                }
        }
    }
}
