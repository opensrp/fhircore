/*
 * Copyright 2021 Ona Systems, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.smartregister.fhircore.engine.data.remote.shared.interceptor

import android.content.Context
import okhttp3.Interceptor
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import timber.log.Timber

class OAuthInterceptor(val context: Context) : Interceptor {

  override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
    Timber.i("Intercepted request for auth headers if needed")

    var request = chain.request()

    val segments = mutableListOf("protocol", "openid-connect", "token")

    if (!request.url.pathSegments.containsAll(segments)) {
      val token = SecureSharedPreference(context).retrieveSessionToken()
      if (token.isNullOrEmpty()) throw IllegalStateException("No session token found")

      Timber.i("Passing auth token for %s", request.url.toString())

      request = request.newBuilder().addHeader("Authorization", "Bearer $token").build()
    }

    return chain.proceed(request)
  }
}
