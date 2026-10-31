// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.reddit.drawer

import app.reseam.runtime.Export

/** The community drawer rows the drawer patch tells apart. */
@Export
enum class DrawerRow {
    DIVIDER,
    HEADER,
    REDDIT_PRO,
    RESOURCES,
    POPULAR,
    NEWS,
    LATEST,
}
