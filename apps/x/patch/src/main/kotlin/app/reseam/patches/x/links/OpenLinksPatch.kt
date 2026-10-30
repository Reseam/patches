// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.x.links

import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.before
import app.reseam.patch.dex.Opcode
import app.reseam.patch.dex.fieldRef
import app.reseam.patch.dex.opcode
import app.reseam.patch.fieldTarget
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.methodTarget
import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.x.core.X
import app.reseam.patches.x.core.xSettings
import app.reseam.x.links.bindings.Links
import app.reseam.x.settings.XSettings
import app.reseam.x.settings.XStrings

val openLinks = patch("Open full links") {
    description("Opens a link's real destination instead of routing it through t.co.")
    compatibleWith(X)
    settings(xSettings, section(XStrings.sectionLinks, XSettings.canonicalLinks))

    execute {
        urlEntity.classDef.directMethods.filter { it.name == "<init>" && it.instructionCount > 0 }.forEach { constructor ->
            methodTarget("urlEntityInit:${constructor.descriptor}") { constructor }
                .after { Links.record(thisObject.field(tcoUrl), thisObject.field(expandedUrl)) }
        }
        urlOpener.before { param(0).assign(Links.canonical(param(0))) }
    }
}

// Adds X's click-measurement query params to a link before it opens; param 0 is the url.
private val urlOpener = method("urlFinalizer") {
    strings("DCM")
    returns(Type.String)
    custom { parameterTypes.size == 3 && parameterTypes[0] == Type.String && parameterTypes[2] == Type.Boolean }
}

private val urlEntity = klass("urlEntity") { strings("UrlEntity(displayUrl=") }

// The link handler reads the two UrlEntity url fields in order: expandedUrl for the host check,
// then url (t.co). UrlEntity's own toString reorders them, so take them from the handler.
private val linkHandler = method("linkHandler") { strings("MediaEntity should have been removed") }
private val expandedUrl = fieldTarget("expandedUrl") { urlFields()[0] }
private val tcoUrl = fieldTarget("tcoUrl") { urlFields()[1] }

private fun urlFields() =
    linkHandler.method.instructions
        .filter { it.opcode == Opcode.IGET_OBJECT && it.fieldRef?.definingClass == urlEntity.descriptor }
        .mapNotNull { it.fieldRef }
        .distinct()
