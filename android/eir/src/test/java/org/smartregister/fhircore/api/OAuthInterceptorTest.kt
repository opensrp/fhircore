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

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Request
import org.junit.Assert
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.smartregister.fhircore.FhirApplication
import org.smartregister.fhircore.RobolectricTest
import org.smartregister.fhircore.auth.secure.Credentials
import org.smartregister.fhircore.auth.secure.FakeKeyStore
import org.smartregister.fhircore.auth.secure.SecureConfig

class OAuthInterceptorTest : RobolectricTest() {

  private lateinit var interceptor: OAuthInterceptor

  @Before
  fun setUp() {
    val creds = Credentials("demo", CharArray(1), "dummy_token")
    SecureConfig(FhirApplication.getContext()).saveCredentials(creds)
    interceptor = OAuthInterceptor(FhirApplication.getContext())
  }

  @Test
  fun testVerifyRequestBuildingProcess() {
    val chain = mockk<Interceptor.Chain>()
    val url =
      HttpUrl.Builder()
        .scheme("https")
        .host("localhost")
        .addPathSegment("protocol")
        .addPathSegment("openid-connect")
        .addPathSegment("patients")
        .build()
    val slot = slot<Request>()

    every { chain.request() } answers { Request.Builder().url(url).build() }
    every { chain.proceed(capture(slot)) } returns mockk()

    interceptor.intercept(chain)

    val request = slot.captured

    val httpUrl = request.url
    val authHeader = request.header("Authorization")

    Assert.assertEquals("https", httpUrl.scheme)
    Assert.assertEquals("localhost", httpUrl.host)
    Assert.assertEquals(3, httpUrl.pathSegments.size)
    Assert.assertEquals("Bearer dummy_token", authHeader)

    verify(exactly = 1) { chain.proceed(any()) }
  }

  companion object {
    @JvmStatic
    @BeforeClass
    fun beforeClass() {
      FakeKeyStore.setup
    }
  }
}
