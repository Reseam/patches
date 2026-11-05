// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.x.core

import app.reseam.patch.before
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.x.api.bindings.XApi

// Every client X talks to its API with is an OkHttp client, and OkHttp keeps its names, so one
// interceptor sees each request and response on every screen, before Apollo or Retrofit parse it.
val xApi = patch {
    compatibleWith(X)

    execute {
        clientBuild.before { XApi.install(thisObject) }

        // okio is renamed, so the extension can't name the buffer that reads a request's body.
        XApi.requestBytes.implement {
            val buffer = newInstance(okioBuffer.descriptor)
            param(0).call(requestBodyWriteTo, buffer)
            returnValue(buffer.call(bufferReadBytes))
        }
    }
}

private val clientBuild = klass($$"okhttp3.OkHttpClient$Builder").method("build")
private val requestBodyWriteTo = klass("okhttp3.RequestBody").method("writeTo")
private val responseBodySource = klass("okhttp3.ResponseBody").method("source")

// okio's Buffer is the class that is both the sink a request body writes to and the source a response body reads.
private val okioBuffer = klass("okioBuffer") {
    implements(requestBodyWriteTo.parameterTypes.single())
    implements(responseBodySource.returnType)
}

private val bufferReadBytes = okioBuffer.methods("bufferReadBytes") { params(); returns("[B") }.single()
