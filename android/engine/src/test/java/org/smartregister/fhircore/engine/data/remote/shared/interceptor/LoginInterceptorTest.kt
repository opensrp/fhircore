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

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import okhttp3.Interceptor
import okhttp3.Request
import org.junit.Test

class LoginInterceptorTest {

  private val loginInterceptor = spyk<LoginInterceptor>()

  private val interceptorChain = mockk<Interceptor.Chain>(relaxed = true)

  @Test
  fun testIntercept() {
    val chainedRequest = mockk<Request>()
    every { interceptorChain.request() } returns chainedRequest
    loginInterceptor.intercept(interceptorChain)
    verify { interceptorChain.proceed(chainedRequest) }
  }
}
