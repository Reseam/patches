// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.reddit.settings

import app.reseam.runtime.Export

@Export
class ReseamSettingsIcon : (Any, Any) -> Unit? {
    override fun invoke(composer: Any, changed: Any): Unit? {
        draw(composer)
        return unit()
    }

    // Protected @JvmStatic hooks make Kotlin calls use the outer static methods edited by the
    // patch.
    companion object {
        @Export @JvmStatic protected fun draw(composer: Any) {}

        // Nullable Unit keeps the JVM return type kotlin.Unit instead of void.
        @Export
        @JvmStatic
        protected external fun unit(): Unit?
    }
}
