// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.x.core

import app.reseam.patch.MethodTarget
import app.reseam.patch.Type
import app.reseam.patch.dex.fieldRef
import app.reseam.patch.dex.ref
import app.reseam.patch.fieldTarget
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.x.posts.bindings.XPosts

// A post's fields are found by the toString labels they follow, and each value inside them by its
// declared type, so nothing names an obfuscated class or field.
val xPosts = patch {
    compatibleWith(X)

    execute {
        val post = availablePost.descriptor
        XPosts.postId.implement {
            returnValue(param(0).cast(post).field(idField).cast(idField.type).field(idValue))
        }
        XPosts.postText.implement {
            val note = param(0).cast(post).field(notePostField)
            whenNotNull(note) {
                val text = note.cast(notePostField.type).field(notePostText)
                whenNotNull(text) { returnValue(text) }
            }
            returnValue(param(0).cast(post).field(textField))
        }
        XPosts.authorId.implement {
            val author = param(0).cast(post).field(authorField)
            whenNotNull(author) {
                val id = author.callInterface(authorField.type, "getId", "()$authorIdType")
                whenNotNull(id) { returnValue(id.cast(authorIdType).field(authorIdValue)) }
            }
            returnValue(long(0))
        }
        XPosts.authorName.implement {
            val author = param(0).cast(post).field(authorField)
            whenNotNull(author) { returnValue(author.callInterface(authorField.type, "getName", "()${Type.String}")) }
            returnNull()
        }
        XPosts.authorScreenName.implement {
            val author = param(0).cast(post).field(authorField)
            whenNotNull(author) { returnValue(author.callInterface(authorField.type, screenNameGetter.name, "()${Type.String}")) }
            returnNull()
        }
        XPosts.postedAtSeconds.implement {
            val time = param(0).cast(post).field(timeField)
            whenNotNull(time) { returnValue(time.cast(timeField.type).field(timeSeconds)) }
            returnValue(long(0))
        }
        XPosts.postMedia.implement {
            val media = param(0).cast(post).field(mediaState)
            whenInstanceOf(media, regularMedia.descriptor) { returnValue(media.cast(regularMedia.descriptor).field(mediaList)) }
            returnNull()
        }
        XPosts.photoUrl.implement {
            whenInstanceOf(param(0), mediaPhoto.descriptor) { returnValue(call(photo4kUrl, param(0).cast(mediaPhoto.descriptor))) }
            returnNull()
        }
    }
}

private val postToString = method("availablePostToString") {
    inClass(availablePost)
    name("toString")
}

private val idField = fieldAfter(postToString, ", postId=")
private val authorField = fieldAfter(postToString, ", author=")
private val textField = fieldAfter(postToString, ", text=")
private val timeField = fieldAfter(postToString, ", timestamp=")
private val notePostField = fieldAfter(postToString, ", notePostState=")

// A long post's text field holds only what fits before "Show more"; its note post holds all of it.
private val notePostToString = method("notePostStateToString") {
    name("toString")
    strings("NotePostState(text=")
}
private val notePostText = fieldAfter(notePostToString, "NotePostState(text=")

// The id inside the post id and user id value classes, and the epoch seconds inside Instant, are each
// the only long field of their class.
private val idValue = onlyLongField("postIdValue") { idField.type }
private val timeSeconds = onlyLongField("timestampSeconds") { timeField.type }
private val authorIdType get() = klass(authorField.type).classDef.method("getId")?.returnType ?: error("author getId missing")
private val authorIdValue = onlyLongField("authorIdValue") { authorIdType }

// The author is an interface whose getters R8 renames; MinimalUser implements it, and its toString
// passes the screen name to an outlined append right after the label.
private const val SCREEN_NAME_LABEL_ARGUMENT = 2
private val minimalUser = klass("minimalUser") { strings("MinimalUser(id=") }
private val minimalUserToString = method("minimalUserToString") {
    inClass(minimalUser)
    name("toString")
}
private val screenNameField = minimalUserToString.point {
    invokeStatic()
    argument(SCREEN_NAME_LABEL_ARGUMENT) { string(", screenName=") }
}.writer(SCREEN_NAME_LABEL_ARGUMENT + 1).field("minimalUserScreenName")
private val screenNameGetter = method("authorScreenName") {
    inClass(minimalUser)
    params()
    returns(Type.String)
    accesses { owner(minimalUser.descriptor); name(screenNameField.name) }
    custom { klass(authorField.type).classDef.method(name, proto) != null }
}

private val regularMedia = klass("regularPostMedia") { strings("RegularMedia(media=") }
private val mediaState = fieldTarget("postMediaState") {
    availablePost.classDef.instanceFields.single { it.fieldType == regularMedia.classDef.interfaces.single() }.ref
}
private val regularMediaToString = method("regularMediaToString") {
    inClass(regularMedia)
    name("toString")
}
private val mediaList = fieldAfter(regularMediaToString, "RegularMedia(media=")

private val photo4kUrl = method("photo4kUrl") {
    strings("https://pbs.twimg.com/media/", "4096x4096")
    param(0, mediaPhoto.descriptor)
}

private fun fieldAfter(toString: MethodTarget, label: String) =
    toString.point { string(label) }.next { where { fieldRef != null } }.field()

private fun onlyLongField(name: String, owner: () -> String) = fieldTarget(name) {
    klass(owner()).classDef.instanceFields.single { it.fieldType == Type.Long }.ref
}
