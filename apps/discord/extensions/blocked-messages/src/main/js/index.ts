// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

import { RowType } from "discord:modules/messages/native/renderer/RowGeneratorConstants.tsx";

export function isBlockedGroup(row: { type: number }) {
    return row.type === RowType.BLOCKED_GROUP || row.type === RowType.IGNORED_GROUP;
}
