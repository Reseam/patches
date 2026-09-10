// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.instagram.interaction

import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.classTarget
import app.reseam.patch.descriptor
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.methodTarget
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.instagram.core.FRIENDSHIP_STATUS
import app.reseam.patches.instagram.core.FollowSettings
import app.reseam.patches.instagram.core.FollowsYouIndicator
import app.reseam.patches.instagram.core.INSTAGRAM
import app.reseam.patches.instagram.core.PANDO_FRIENDSHIP_STATUS
import app.reseam.patches.instagram.core.USER_SESSION
import app.reseam.patches.instagram.core.instagramSettings
import app.reseam.patches.instagram.core.signatureCheck

val followsYou = patch("Follows you indicator") {
    description("Shows a 'Follows you' badge next to usernames in search results")
    compatibleWith(INSTAGRAM)
    dependsOn(signatureCheck)
    settings(instagramSettings, section("Social", FollowSettings.followsYouIndicator))

    execute {
        searchSubtitleBuilder.after {
            val updated = call(FollowsYouIndicator.appendFromSession, capture("result"), param(1), param(3))
            returnValue(updated)
        }

        FollowsYouIndicator.appendFromSession.implement {
            val subtitle = param(0)
            val userSession = param(1).cast(USER_SESSION)
            val userViewModel = param(2).cast(userViewModelClass.descriptor)
            val relation = call(userRelationGetter, userSession, userViewModel)
            whenNotNull(relation) {
                val status = call(friendshipStatusExtractor, relation)
                whenNotNull(status) {
                    val followedBy = status.callInterface(FRIENDSHIP_STATUS, pandoFollowsViewer.name, "()Ljava/lang/Boolean;")
                    returnValue(call(FollowsYouIndicator.maybeAppend, subtitle, followedBy))
                }
            }
            returnValue(subtitle)
        }
    }
}

val searchRowBinder = method("searchRowBinder") {
    strings("search_navigate_to_user", " • ")
    returns(Type.Void)
}

// Several String builders called by the row binder take Context + UserSession. The canonical
// one has five parameters whose tail is obfuscated app types (the user view model and relation
// helpers); the `LX/` prefix catches them without naming any.
val searchSubtitleBuilders = methods("searchSubtitleBuilders") {
    calledBy(searchRowBinder)
    returns(Type.String)
    hasParam(Type.Context)
    hasParam(USER_SESSION)
}

val searchSubtitleBuilder = methodTarget("searchSubtitleBuilder") {
    searchSubtitleBuilders.single {
        val params = parameterTypes
        params.size == 5 &&
            params[0] == Type.Context &&
            params[1] == descriptor(USER_SESSION) &&
            params.drop(2).all { it.startsWith("LX/") }
    }.method
}

// The subtitle builder's fourth parameter is the user view model, whose static
// relationGetter(UserSession, Self) returns the relation carrying the friendship status.
val userViewModelClass = classTarget("userViewModelClass") {
    bytecode.findClass(searchSubtitleBuilder.parameterTypes[3]) ?: error("user view model class missing")
}

val userRelationGetter = method("userRelationGetter") {
    inClass(userViewModelClass)
    params(USER_SESSION, userViewModelClass.descriptor)
}

val friendshipStatusExtractor = method("friendshipStatusExtractor") {
    returns(FRIENDSHIP_STATUS)
    params(userRelationGetter.returnType)
}

// The Pando (Meta GraphQL) implementation identifies each field by String.hashCode of its
// GraphQL key, which pins the "follows the viewer" accessor across renames.
val pandoFollowsViewer = method("pandoFollowsViewer") {
    inClass(klass(PANDO_FRIENDSHIP_STATUS))
    returns("java.lang.Boolean")
    params()
    literals("followed_by".hashCode().toLong())
}
