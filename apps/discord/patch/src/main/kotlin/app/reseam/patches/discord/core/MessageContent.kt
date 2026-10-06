// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.discord.core

import app.reseam.patch.function

/** Builds one chat row from a message; patches that change how messages look wrap it. */
internal val createMessageContent = function {
    name("createMessageContent")
    strings("renderGiftCode", "animatingStickerMessageId", "createActivityInstanceEmbed")
    paramCount(1)
}
