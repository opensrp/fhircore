/*
 * Copyright 2021-2024 Ona Systems, Inc
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

package org.smartregister.fhircore.engine.configuration.customsearch

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.SearchParameter
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class SearchParametersConfigServiceTest {

  private lateinit var searchParametersConfigService: SearchParametersConfigService
  private lateinit var searchParameterConfigStore: ISearchParametersConfigStore
  private val configStoreSearchParameter: SearchParameter =
    SearchParameter().apply { code = "config-store-test-search" }

  @Before
  fun setUp() {
    searchParameterConfigStore = spyk {
      every { read() } returns
        Bundle().apply {
          addEntry(
            Bundle.BundleEntryComponent().apply { resource = configStoreSearchParameter },
          )
        }
      coEvery { write(any()) } just runs
    }
    searchParametersConfigService = SearchParametersConfigService(searchParameterConfigStore)
  }

  @Test
  fun getCustomSearchParameters() {
    val result = searchParametersConfigService.getCustomSearchParameters()
    verify { searchParameterConfigStore.read() }
    Assert.assertTrue(configStoreSearchParameter in result)
  }

  @Test
  fun getCustomSearchParametersReturnsDefaultSearchParams() {
    val result = searchParametersConfigService.getCustomSearchParameters()
    verify { searchParameterConfigStore.read() }
    Assert.assertTrue(result[0].url.contains("group-active"))
    Assert.assertTrue(result[1].url.contains("flag-status"))
    Assert.assertTrue(result[2].url.contains("medication-sort"))
    Assert.assertTrue(result[3].url.contains("patient-search"))
  }

  @Test
  fun saveBundle() = runTest {
    val searchParamBundle = Bundle()
    searchParametersConfigService.saveBundle(searchParamBundle)
    coVerify { searchParameterConfigStore.write(searchParamBundle) }
  }
}
