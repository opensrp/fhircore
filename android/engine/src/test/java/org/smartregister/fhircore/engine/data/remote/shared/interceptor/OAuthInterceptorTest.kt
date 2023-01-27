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

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import okhttp3.Interceptor
import okhttp3.Request
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.smartregister.fhircore.engine.auth.TokenManagerService
import org.smartregister.fhircore.engine.robolectric.FhircoreTestRunner
import org.smartregister.fhircore.engine.robolectric.RobolectricTest

@RunWith(FhircoreTestRunner::class)
@Config(sdk = [29])
class OAuthInterceptorTest : RobolectricTest() {

  @Test
  fun testInterceptShouldAddTokenHeader() {
    val context = ApplicationProvider.getApplicationContext<Application>()

    val tokenManagerService = mockk<TokenManagerService>()
    val interceptor = OAuthInterceptor(context, tokenManagerService)
    every { tokenManagerService.getActiveAuthToken() } returns "my-access-token"

    val requestBuilder = spyk(Request.Builder())
    val request = spyk(Request.Builder().url("http://test-url.com").build())
    val chain = mockk<Interceptor.Chain>()
    every { chain.request() } returns request
    every { request.newBuilder() } returns requestBuilder.url("http://test-url.com")
    every { chain.proceed(any()) } returns mockk()

    interceptor.intercept(chain)

    verify { requestBuilder.addHeader("Authorization", "Bearer my-access-token") }
  }
}
