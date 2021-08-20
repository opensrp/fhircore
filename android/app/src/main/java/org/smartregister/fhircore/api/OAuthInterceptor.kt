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

package org.smartregister.fhircore.api

import android.content.Context
import okhttp3.Interceptor
import org.smartregister.fhircore.auth.secure.SecureConfig
import timber.log.Timber

class OAuthInterceptor(context: Context) : Interceptor {
  private val mContext: Context = context

  override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
    Timber.v("Intercepted request for auth headers if needed")

    var request = chain.request()

    if (!request.url.pathSegments.containsAll(mutableListOf("protocol", "openid-connect", "token"))
    ) {
      val token = SecureConfig(mContext).retrieveSessionToken()
      if (token.isNullOrEmpty()) throw IllegalStateException("No session token found")

      Timber.v("Passing auth token for %s", request.url.toString())

      request = request.newBuilder().addHeader("Authorization", "Bearer $token").build()
    }

    return chain.proceed(request)
  }
}
