// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.instagram.interaction

import app.reseam.instagram.follows.bindings.FollowsYouIndicator
import app.reseam.instagram.settings.FollowSettings
import app.reseam.instagram.settings.InstagramStrings
import app.reseam.patch.Type
import app.reseam.patch.before
import app.reseam.patch.descriptor
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.section
import app.reseam.patches.instagram.InstagramPatchStrings
import app.reseam.patches.instagram.core.INSTAGRAM
import app.reseam.patches.instagram.core.instagramSettings
import app.reseam.patches.instagram.core.signatureCheck

private const val USER = "com.instagram.user.model.User"
private const val RELATIONSHIP = "com.instagram.api.schemas.RelationshipInfoDict"
private const val PANDO_RELATIONSHIP = "com.instagram.api.schemas.ImmutablePandoRelationshipInfoDict"
private const val USER_DETAIL_VIEW_MODEL = "com.instagram.profile.fragment.UserDetailViewModel"

val followsYou = patch(InstagramPatchStrings.followsYou) {
    description(InstagramPatchStrings.followsYouDescription)
    compatibleWith(INSTAGRAM)
    dependsOn(signatureCheck)
    settings(instagramSettings, section(InstagramStrings.sectionSocial, FollowSettings.followsYouIndicator))

    execute {
        FollowsYouIndicator.followedBy.implement {
            val relation = param(0).cast(USER).call(userRelationship)
            whenNotNull(relation) {
                returnValue(relation.callInterface(RELATIONSHIP, pandoFollowedBy.name, "()Ljava/lang/Boolean;"))
            }
            returnNull()
        }

        profileUserChanged.before { FollowsYouIndicator.rememberProfileUser(param(0), param(1)) }
        // The header binder gets the profile's view model through one of its parameters.
        val headerContext = profileHeaderBinder.parameterTypes.single { type ->
            bytecode.findClass(type)?.instanceFields?.any { it.fieldType == descriptor(USER_DETAIL_VIEW_MODEL) } == true
        }
        profileFullName.before {
            val viewModel = paramOfType(headerContext).fieldOfType(USER_DETAIL_VIEW_MODEL)
            capture("fullName").assign(FollowsYouIndicator.profileName(capture("fullName"), viewModel))
        }
    }
}

// The profile view model receives each new copy of the user it shows.
private val profileUserChanged = method("profileUserChanged") {
    inClass(klass(USER_DETAIL_VIEW_MODEL))
    params(USER_DETAIL_VIEW_MODEL, USER)
    returns(Type.Void)
}

private val profileHeaderBinder = method("profileHeaderBinder") { strings("bindFullName") }

private val profileFullName = profileHeaderBinder
    .point { string("bindFullName") }
    .next { invokeVirtual { owner("android.widget.TextView"); name("setText") } }
    .captureArgumentAs("fullName", 1, Type.String)

private val userRelationship = method("userRelationship") {
    inClass(klass(USER))
    returns(RELATIONSHIP)
    params()
}

private val pandoFollowedBy = method("pandoFollowedBy") {
    inClass(klass(PANDO_RELATIONSHIP))
    returns("java.lang.Boolean")
    params()
    literals("followed_by".hashCode().toLong())
}
