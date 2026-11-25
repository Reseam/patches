// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

type ShelfLayout = { TABS_BAR_MARGIN_TOP?: number };

/** Removes the gap above Home's tabs from the stories shelf's [layout] constants, so Home stops sliding its top bar up by it. */
export function removeShelfGap(layout: ShelfLayout | null | undefined) {
    // A write to a frozen object throws in strict mode, inside the module's factory.
    if (layout != null && !Object.isFrozen(layout)) layout.TABS_BAR_MARGIN_TOP = 0;
}
