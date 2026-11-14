// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.x.postheader

import app.reseam.runtime.Export

// The factory and update functions X's AndroidView calls for the AI labels, like DownloadClick. The patch
// emits the bodies.

@Export
class TextLabelFactory : (Any?) -> Any? {
    @Export override external fun invoke(context: Any?): Any?
}

@Export
class TextLabelUpdate(@JvmField val post: Any?) : (Any?) -> Unit? {
    @Export override external fun invoke(view: Any?): Unit?
}

@Export
class ImageLabelFactory : (Any?) -> Any? {
    @Export override external fun invoke(context: Any?): Any?
}

@Export
class ImageLabelUpdate(@JvmField val photo: Any?) : (Any?) -> Unit? {
    @Export override external fun invoke(view: Any?): Unit?
}
