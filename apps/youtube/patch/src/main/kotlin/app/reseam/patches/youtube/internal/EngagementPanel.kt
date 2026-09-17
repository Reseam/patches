// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.internal

import app.reseam.patch.ExtClass
import app.reseam.patch.Type
import app.reseam.patch.before
import app.reseam.patch.classTarget
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.Opcode
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.youTubeSettings

private const val CONTROLLER_UNINITIALISED =
    "EngagementPanelController: cannot show EngagementPanel before EngagementPanelController.init() has been called."

private const val OPTIONAL = "j\$.util.Optional"

// The flag the controller reads while deciding how to present the panel it just resolved.
private const val PANEL_PRESENTATION_FLAG = 45615449L

/**
 * Tracks which engagement panel, the sheets that slide up under the player for the description,
 * comments and chapters, is open. Patches that hide something only inside one of them ask the
 * extension.
 */
val engagementPanelHook = patch {
    compatibleWith(YOUTUBE)
    dependsOn(youTubeSettings)

    execute {
        // Past the null check on the resolved panel, so the identifier is always there to read.
        engagementPanelShown.before { call(EngagementPanel.open, capture("panel").field(engagementPanelId)) }
        engagementPanelDismissed.before { call(EngagementPanel.close) }
    }
}

object EngagementPanel : ExtClass("app.reseam.youtube.player.EngagementPanel") {
    val open = static("open", Type.String)
    val close = static("close")
}

// The controller refuses to show a panel before it is initialised, and says so.
val engagementPanelShow = method("engagementPanelShow") { strings(CONTROLLER_UNINITIALISED) }

val engagementPanelController = classTarget("engagementPanelController") {
    bytecode.findClass(engagementPanelShow.owner) ?: error("The engagement panel controller class is missing")
}

/**
 * The panel the request named is looked up through two optionals and cast to the panel type. The
 * cast is what names the register, and the null check right after it is what makes the register
 * safe to read from.
 */
val engagementPanelShown = engagementPanelShow
    .point("engagementPanelShown") { string(CONTROLLER_UNINITIALISED) }
    .next { invokeVirtual { owner(OPTIONAL); name("orElse") } }
    .next { invokeVirtual { owner(OPTIONAL); name("orElse") } }
    .next { opcode(Opcode.CHECK_CAST) }
    .captureAs("panel")
    .next { opcode(Opcode.IF_EQZ) }
    .next { opcode(Opcode.IGET_OBJECT) }

// The same method later reads the panel's identifier to queue it; that read names the field.
val engagementPanelId = engagementPanelShow
    .point("engagementPanelId") { literal(PANEL_PRESENTATION_FLAG) }
    .next { invokeVirtual { owner("java.util.ArrayDeque"); name("iterator") } }
    .next { opcode(Opcode.IGET_OBJECT); field { type(Type.String) } }
    .field()

// The one method that takes a panel and a flag and tears it down; it hands the activity back.
val engagementPanelDismissed = method("engagementPanelDismissed") {
    inClass(engagementPanelController)
    flags(AccessFlags.PRIVATE or AccessFlags.FINAL)
    returns(Type.Void)
    params(engagementPanelId.owner, Type.Boolean)
}
