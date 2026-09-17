// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.interaction

import app.reseam.patch.ExtClass
import app.reseam.patch.Type
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.Opcode
import app.reseam.patch.dex.fieldRef
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.after
import app.reseam.patch.settings.before
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettings
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings

private const val ALERT_DIALOG = "android.app.AlertDialog"
private const val ALERT_DIALOG_BUILDER = "android.app.AlertDialog\$Builder"

val removeViewerDiscretionDialog = patch("Remove viewer discretion dialog") {
    description(
        """
            Accepts the age-restriction warning for you instead of showing it.
            It does not bypass the restriction itself.
        """,
    )
    compatibleWith(YOUTUBE)
    settings(youTubeSettings, section(YouTubeSettingsPages.Video, "Age restriction", YouTubeSettings.removeViewerDiscretionDialog))

    execute {
        // Which dialog is on screen is not knowable from the dialog itself, so the extension keeps
        // the last playability status and only dismisses the dialogs that status can produce.
        playabilityGuard.before(YouTubeSettings.removeViewerDiscretionDialog) {
            call(ViewerDiscretionDialog.setPlayabilityStatus, param(0))
        }

        dialogShown.after(YouTubeSettings.removeViewerDiscretionDialog) {
            call(ViewerDiscretionDialog.confirm, capture("dialog"))
        }

        // Tracked points remain attached to their instructions as either hook is emitted.
        modernDialogCreated.after(YouTubeSettings.removeViewerDiscretionDialog) {
            call(ViewerDiscretionDialog.confirm, capture("dialog"))
        }
        modernDialogChosen.after(YouTubeSettings.removeViewerDiscretionDialog) {
            capture("modern").assign(call(ViewerDiscretionDialog.useModernDialog, capture("modern")))
        }
    }
}

// The playability status the player response reports; the enum names its constants.
val playabilityStatus = klass("playabilityStatus") {
    strings("GL_PLAYBACK_REQUIRED")
}

// The normal-response guard compares one status; the other predicate accepts several.
val playabilityGuard = method("playabilityGuard") {
    flags(AccessFlags.STATIC)
    params(playabilityStatus.descriptor)
    returns(Type.Boolean)
    custom { instructions.count { it.fieldRef?.definingClass == playabilityStatus.descriptor } == 1 }
}

// The plain dialog: built, created, and shown in one method.
val viewerDiscretionDialog = method("viewerDiscretionDialog") {
    returns(Type.Void)
    paramCount(3)
    hasParam(Type.String)
    calls { owner("Landroid/app/AlertDialog\$Builder;"); name("setNegativeButton") }
    calls { owner("Landroid/app/AlertDialog\$Builder;"); name("setOnCancelListener") }
    calls { owner("Landroid/app/AlertDialog\$Builder;"); name("create") }
    calls { owner("Landroid/app/AlertDialog;"); name("show") }
}

// The dialog is stashed in a field and read back to be shown; that read is what names the register.
val dialogShown = viewerDiscretionDialog
    .point("dialogShown") { opcode(Opcode.IGET_OBJECT); field { type(ALERT_DIALOG) } }
    .captureAs("dialog")
    .next { invokeVirtual { owner(ALERT_DIALOG); name("show") } }

// The modern dialog: the same method builds either style, and shows whichever it built elsewhere.
val modernDialogBuilder = method("modernDialogBuilder") {
    flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
    returns(Type.Void)
    params()
    calls { owner("Landroid/app/AlertDialog\$Builder;"); name("setIcon") }
    calls { owner("Landroid/app/AlertDialog\$Builder;"); name("create") }
}

// The first thing the method does is ask whether the modern style is available.
val modernDialogChosen = modernDialogBuilder
    .point("modernDialogChosen") { opcode(Opcode.MOVE_RESULT) }
    .captureAs("modern", Type.Boolean)

// Nothing here shows the plain dialog, so it is confirmed as soon as it exists.
val modernDialogCreated = modernDialogBuilder
    .point("modernDialogCreated") { invokeVirtual { owner(ALERT_DIALOG_BUILDER); name("create") } }
    .next { resultOf(ALERT_DIALOG) }
    .captureAs("dialog", ALERT_DIALOG)

object ViewerDiscretionDialog : ExtClass("app.reseam.youtube.misc.ViewerDiscretionDialog") {
    val setPlayabilityStatus = static("setPlayabilityStatus", "java.lang.Enum")
    val confirm = static("confirm", ALERT_DIALOG)
    val useModernDialog = static("useModernDialog", Type.Boolean, returns = Type.Boolean)
}
