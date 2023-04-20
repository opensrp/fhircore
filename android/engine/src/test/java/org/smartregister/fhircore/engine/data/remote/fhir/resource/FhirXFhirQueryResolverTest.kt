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

package org.smartregister.fhircore.engine.data.remote.fhir.resource

import com.google.android.fhir.FhirEngine
import com.google.android.fhir.search.search
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.rule.CoroutineTestRule

@OptIn(ExperimentalCoroutinesApi::class)
class FhirXFhirQueryResolverTest {

  @get:Rule(order = 1) val coroutineTestRule = CoroutineTestRule()

  private val fhirEngine: FhirEngine = mockk()
  private val xFhirQueryResolver = FhirXFhirQueryResolver(fhirEngine)

  @Test
  fun resolve() = runTest {
    val xFhirQueryString = "Patient?active=true&organization="
    mockkStatic("com.google.android.fhir.search.SearchKt")
    coEvery { any<FhirEngine>().search(any<String>()) } returns emptyList()
    xFhirQueryResolver.resolve(xFhirQueryString)
    coVerify { any<FhirEngine>().search(withArg<String> { xFhirQueryString.contains(it) }) }
  }
}
