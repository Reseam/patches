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
import app.reseam.patches.instagram.core.FollowSettings
import app.reseam.patches.instagram.core.FollowsYouIndicator
import app.reseam.patches.instagram.core.INSTAGRAM
import app.reseam.patches.instagram.core.USER_SESSION
import app.reseam.patches.instagram.core.instagramSettings
import app.reseam.patches.instagram.core.signatureCheck

private const val USER = "com.instagram.user.model.User"
private const val RELATIONSHIP = "com.instagram.api.schemas.RelationshipInfoDict"
private const val PANDO_RELATIONSHIP = "com.instagram.api.schemas.ImmutablePandoRelationshipInfoDict"

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
            val cache = call(userCacheProviderGetter, userSession)
            val user = userViewModel.call(userFromSearchModel, cache)
            whenNotNull(user) {
                val relation = user.call(userRelationship)
                whenNotNull(relation) {
                    val followedBy = relation.callInterface(RELATIONSHIP, pandoFollowedBy.name, "()Ljava/lang/Boolean;")
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

val userViewModelClass = classTarget("userViewModelClass") {
    bytecode.findClass(searchSubtitleBuilder.parameterTypes[3]) ?: error("user view model class missing")
}

val userCacheProvider = klass("userCacheProvider") {
    hasInstanceField(USER_SESSION)
    hasInstanceField("com.instagram.feed.media.MediaCache")
    hasInstanceField("com.instagram.user.model.UserCache")
    implements(userFromSearchModel.parameterTypes.single())
}

val userCacheProviderGetter = method("userCacheProviderGetter") {
    inClass(userCacheProvider)
    returns(userCacheProvider.descriptor)
    params(USER_SESSION)
}

val userFromSearchModel = method("userFromSearchModel") {
    inClass(userViewModelClass)
    returns(USER)
    custom { parameterTypes.size == 1 }
}

val userRelationship = method("userRelationship") {
    inClass(klass(USER))
    returns(RELATIONSHIP)
    params()
}

val pandoFollowedBy = method("pandoFollowedBy") {
    inClass(klass(PANDO_RELATIONSHIP))
    returns("java.lang.Boolean")
    params()
    literals("followed_by".hashCode().toLong())
}
