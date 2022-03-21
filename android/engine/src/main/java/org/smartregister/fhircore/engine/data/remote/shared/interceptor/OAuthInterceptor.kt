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
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import okhttp3.Interceptor
import okhttp3.Request
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.auth.TokenManagerService
import timber.log.Timber

class OAuthInterceptor
@Inject
constructor(
  @ApplicationContext val context: Context,
  val tokenManagerService: TokenManagerService
) : Interceptor {

  override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
    var request = chain.request()
    val segments = mutableListOf("protocol", "openid-connect", "token")
    if (!request.hasPaths(segments) && !request.hasOpenResources()) {
      tokenManagerService.runCatching { getBlockingActiveAuthToken() }.getOrNull()?.let { token ->
        Timber.d("Passing auth token for %s", request.url.toString())
        request = request.newBuilder().addHeader("Authorization", "Bearer $token").build()
      }
    }
    return chain.proceed(request)
  }

  fun Request.hasPaths(paths: List<String>) = this.url.pathSegments.containsAll(paths)

  fun Request.hasOpenResources() =
    this.method.contentEquals(REQUEST_METHOD_GET) &&
      this.url.pathSegments.any {
        it.contentEquals(ResourceType.Composition.name) ||
          it.contentEquals(ResourceType.Binary.name)
      }

  companion object {
    const val REQUEST_METHOD_GET = "GET"
  }
}
