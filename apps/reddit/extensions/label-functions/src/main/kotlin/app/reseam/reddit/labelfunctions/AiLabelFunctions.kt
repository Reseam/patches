// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.reddit.labelfunctions

import app.reseam.runtime.Export

// The factory and update functions Reddit's AndroidView calls for the AI labels. They implement Reddit's own
// kotlin.jvm.functions.Function1, so this module stays off the bundle's relocated stdlib. The patch emits the
// bodies.

@Export
class TextLabelFactory : (Any?) -> Any? {
    @Export override external fun invoke(context: Any?): Any?
}

@Export
class TextLabelUpdate(@JvmField val id: String?) : (Any?) -> Unit? {
    @Export override external fun invoke(view: Any?): Unit?
}

@Export
class ImageLabelFactory : (Any?) -> Any? {
    @Export override external fun invoke(context: Any?): Any?
}

@Export
class ImageLabelUpdate(@JvmField val url: String?) : (Any?) -> Unit? {
    @Export override external fun invoke(view: Any?): Unit?
}
