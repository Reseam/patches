// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later


package app.reseam.patches.instagram.interaction

import app.reseam.patches.instagram.core.FollowSettings
import app.reseam.patches.instagram.core.signatureCheckPatch
import app.reseam.patches.instagram.core.settingsPatch

import app.reseam.patch.bindStub
import app.reseam.patch.classHandle
import app.reseam.patch.compatibleWith
import app.reseam.patch.findMethod
import app.reseam.patch.findMethods
import app.reseam.patch.parameterTypes
import app.reseam.patch.patch
import app.reseam.patch.returnType
import app.reseam.patch.settings.SettingsSection

private const val EXT = "Lapp/reseam/instagram/follows/FollowsYouIndicator;"
private const val USER_SESSION = "Lcom/instagram/common/session/UserSession;"
private const val FRIENDSHIP_STATUS = "Lcom/instagram/user/model/FriendshipStatus;"
private const val PANDO_FRIENDSHIP_STATUS =
    "Lcom/instagram/user/model/ImmutablePandoFriendshipStatus;"
private const val CONTEXT = "Landroid/content/Context;"

// FriendshipStatus's Pando (Meta GraphQL) implementation looks fields up by the
// String.hashCode of their GraphQL key. The hash for the "follows the viewer" field
// is what we need; resolving the interface method by hash is stable across versions
// even when the obfuscated method name changes.
private const val FOLLOWED_BY_FIELD = "followed_by"
private const val FRIENDSHIP_FOLLOWS_VIEWER_PROTO = "()Ljava/lang/Boolean;"

val followsYouPatch = patch(
    name = "Follows you indicator",
    description = "Shows a 'Follows you' badge next to usernames in search results",
    compatibleWith = listOf(compatibleWith("com.instagram.android")),
    settingsHost = settingsPatch,
    dependsOn = listOf(signatureCheckPatch, settingsPatch),
    settings = listOf(
        SettingsSection(
            title = "Social",
            settings = listOf(FollowSettings.FollowsYouIndicator),
        ),
    ),
) {
    extendWith("instagram-follows-you.dex")

    execute { ctx ->
        val rowBinder = ctx.findMethod(debug = "searchRowBinder") {
            strings("search_navigate_to_user", " • ")
            returnType("V")
        }

        // Subtitle builder candidates: any String-returning method called by rowBinder
        // that takes Context + UserSession as known params. There are several such
        // overloads in the search-binding code; we pick the canonical 5-arg one
        // whose tail params are obfuscated app types (the user-view-model + relation
        // helpers). Hardcoding the obfuscated names would break across versions; the
        // `LX/`-prefix heuristic captures them all without naming any.
        val subtitleCandidates = ctx.findMethods(debug = "searchSubtitleBuilders") {
            calledBy(rowBinder)
            returnType("Ljava/lang/String;")
            hasParameter(CONTEXT)
            hasParameter(USER_SESSION)
        }
        val subtitleBuilder = subtitleCandidates.singleOrNull { handle ->
            val params = handle.method.parameterTypes
            params.size == 5 &&
                params[0] == CONTEXT &&
                params[1] == USER_SESSION &&
                params[2].startsWith("LX/") &&
                params[3].startsWith("LX/") &&
                params[4].startsWith("LX/")
        } ?: error(
            "searchSubtitleBuilder not unique among ${subtitleCandidates.size} candidates: " +
                subtitleCandidates.joinToString { handle ->
                    "${handle.classDescriptor}->${handle.methodName}${handle.proto}"
                },
        )

        // The 4th parameter (index 3) of the subtitle builder is the "user view model"
        // value class — the same type whose static `relationGetter(UserSession, Self)`
        // returns the relation we drill into for friendship status.
        val userViewModelType = subtitleBuilder.method.parameterTypes[3]

        // Static method on the user-view-model class that combines (UserSession, Self) into
        // the relation object whose payload carries the FriendshipStatus.
        val userViewModelClass = ctx.classHandle(userViewModelType, debug = "userViewModelClass")
        val relationGetter = ctx.findMethod(debug = "userRelationGetter") {
            inClass(userViewModelClass)
            parameterTypes(USER_SESSION, userViewModelType)
        }
        val relationType = relationGetter.method.returnType

        // Extractor: relation -> FriendshipStatus. FriendshipStatus is a public Instagram
        // type so its descriptor is stable across versions.
        val friendshipExtractor = ctx.findMethod(debug = "friendshipStatusExtractor") {
            returnType(FRIENDSHIP_STATUS)
            parameterTypes(relationType)
        }

        // Resolve the obfuscated FriendshipStatus accessor for "follows the viewer" by
        // looking up its Pando hash. The Pando-backed impl identifies each interface
        // field by `String.hashCode(graphqlFieldName)`; matching the literal pins down
        // the override regardless of how the method gets renamed across versions.
        val followedByHash = FOLLOWED_BY_FIELD.hashCode().toLong()
        val pandoImpl = ctx.classHandle(PANDO_FRIENDSHIP_STATUS, debug = "pandoFriendshipStatus")
        val pandoFollowsViewer = ctx.findMethod(debug = "pandoFollowsViewerImpl") {
            inClass(pandoImpl)
            returnType("Ljava/lang/Boolean;")
            parameterTypes()
            literals(followedByHash)
        }
        val followsViewerName = pandoFollowsViewer.methodName

        subtitleBuilder.after {
            val subtitle = capture("subtitle", "Ljava/lang/String;")
            val updated = staticCall(
                EXT,
                "appendFromSession",
                "(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/String;",
                subtitle,
                parameter(1),
                parameter(3),
            )
            returnObject(updated)
        }

        ctx.bindStub(EXT) {
            method("appendFromSession", "(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/String;") {
                val subtitle = parameter(0)
                val userSession = parameter(1).cast(USER_SESSION)
                val userViewModel = parameter(2).cast(userViewModelType)
                val relation = staticCall(
                    relationGetter.classDescriptor,
                    relationGetter.methodName,
                    relationGetter.proto,
                    userSession,
                    userViewModel,
                )
                ifNotNull(relation) {
                    val status = staticCall(
                        friendshipExtractor.classDescriptor,
                        friendshipExtractor.methodName,
                        friendshipExtractor.proto,
                        relation,
                    )
                    ifNotNull(status) {
                        val followedBy = status.interfaceCall(
                            FRIENDSHIP_STATUS,
                            followsViewerName,
                            FRIENDSHIP_FOLLOWS_VIEWER_PROTO,
                        )
                        returnObject(
                            staticCall(
                                EXT,
                                "maybeAppend",
                                "(Ljava/lang/String;Ljava/lang/Boolean;)Ljava/lang/String;",
                                subtitle,
                                followedBy,
                            )
                        )
                    }
                }
                returnObject(subtitle)
            }
        }

        ctx.log.info(
            "FollowsYouPatch: hooked ${subtitleBuilder.classDescriptor}->${subtitleBuilder.methodName}, " +
                "relationGetter=${relationGetter.classDescriptor}->${relationGetter.methodName}, " +
                "friendshipExtractor=${friendshipExtractor.classDescriptor}->${friendshipExtractor.methodName}, " +
                "followsViewerAccessor=$FRIENDSHIP_STATUS->$followsViewerName " +
                "(resolved via Pando hash for '$FOLLOWED_BY_FIELD')",
        )
    }
}
