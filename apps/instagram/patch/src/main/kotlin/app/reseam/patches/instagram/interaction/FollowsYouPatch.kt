// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later


package app.reseam.patches.instagram.interaction

import app.reseam.patches.instagram.core.FollowSettings
import app.reseam.patches.instagram.core.signatureCheckPatch
import app.reseam.patches.instagram.core.settingsPatch

import app.reseam.patch.bindStub
import app.reseam.patch.compatibleWith
import app.reseam.patch.findMethod
import app.reseam.patch.patch
import app.reseam.patch.settings.SettingsSection

private const val EXT = "Lapp/reseam/instagram/follows/FollowsYouIndicator;"

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
            strings("search_navigate_to_user", " \u2022 ")
            returnType("V")
        }

        val subtitleBuilder = ctx.findMethod(debug = "searchSubtitleBuilder") {
            calledBy(rowBinder)
            returnType("Ljava/lang/String;")
            parameterTypes(
                "Landroid/content/Context;",
                "Lcom/instagram/common/session/UserSession;",
                "LX/E8b;",
                "LX/D3X;",
                "LX/WZN;",
            )
        }

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
                val userSession = parameter(1).cast("Lcom/instagram/common/session/UserSession;")
                val d3x = parameter(2).cast("LX/D3X;")
                val relation = staticCall(
                    "LX/D3X;",
                    "A00",
                    "(Lcom/instagram/common/session/UserSession;LX/D3X;)LX/2ai;",
                    userSession,
                    d3x,
                )
                ifNotNull(relation) {
                    val status = staticCall(
                        "LX/135;",
                        "A0l",
                        "(LX/2ai;)Lcom/instagram/user/model/FriendshipStatus;",
                        relation,
                    )
                    ifNotNull(status) {
                        val followedBy = status.interfaceCall(
                            "Lcom/instagram/user/model/FriendshipStatus;",
                            "Bhk",
                            "()Ljava/lang/Boolean;",
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

        ctx.log.info("FollowsYouPatch: hooked subtitle builder and rebound follow-status bridge")
    }
}
