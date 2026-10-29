// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.pinterest.feed

import app.reseam.patch.ExtMethod
import app.reseam.patch.Type
import app.reseam.patch.dex.Opcode
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.point
import app.reseam.patch.settings.GateScope

private val dynamicFeed = klass("com.pinterest.legacy.feeds.DynamicFeed")

internal val dynamicFeedParsed = dynamicFeed.method("<init>") { params(Type.String, Type.String, Type.List, Type.List) }

internal val feedModels = method("feedModels") {
    inClass(dynamicFeed)
    params()
    returns(Type.List)
}.point { opcode(Opcode.IGET_OBJECT) }.field("feedModels")

private val feedIds = dynamicFeed.method("<init>") { params("android.os.Parcel") }
    .point { invokeVirtual { name("readStringList") } }
    .previous { opcode(Opcode.IPUT_OBJECT) }
    .field("feedIds")

/** Runs [filter] on every parsed DynamicFeed's models and ids while the gate's setting is on. */
internal fun GateScope.filterFeed(filter: ExtMethod) =
    dynamicFeedParsed.after { call(filter, thisObject.field(feedModels), thisObject.field(feedIds)) }
