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

package org.smartregister.fhircore.eir.api

import androidx.test.core.app.ApplicationProvider
import ca.uhn.fhir.context.FhirContext
import java.lang.reflect.Proxy
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.Assert
import org.junit.Test
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.eir.BuildConfig
import org.smartregister.fhircore.eir.RobolectricTest
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class HapiFhirServiceTest : RobolectricTest() {

  @Test
  fun `create() should return HapiFhirService with correct base url, client, interceptors and converter factories`() {
    val parser = FhirContext.forR4().newJsonParser()

    val hapiFhirService =
      HapiFhirService.create(parser, ApplicationProvider.getApplicationContext())

    val retrofit = hapiFhirService.getRetrofitInstance()
    Assert.assertEquals(BuildConfig.FHIR_BASE_URL, retrofit.baseUrl().toString())

    val converterFactories = retrofit.converterFactories()
    Assert.assertTrue(converterFactories.get(1) is FhirConverterFactory)
    Assert.assertTrue(converterFactories.get(2) is GsonConverterFactory)

    val okHttpClient = retrofit.callFactory() as OkHttpClient
    Assert.assertTrue(okHttpClient.interceptors[0] is OAuthInterceptor)
    Assert.assertTrue(okHttpClient.interceptors[1] is HttpLoggingInterceptor)
  }

  private fun HapiFhirService.getRetrofitInstance(): Retrofit {
    return ReflectionHelpers.getField<Retrofit>(Proxy.getInvocationHandler(this), "this$0")
  }
}
