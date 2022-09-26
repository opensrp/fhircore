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

package org.smartregister.fhircore.engine.util.extension

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.logicalId
import com.google.android.fhir.workflow.FhirOperator
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.Composition
import org.junit.Assert
import org.junit.Test
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper

class FhirEngineExtensionTest : RobolectricTest() {

  private val application = ApplicationProvider.getApplicationContext<Application>()

  private val fhirEngine: FhirEngine = mockk()

  private val fhirOperator: FhirOperator = mockk()

  private val sharedPreferenceHelper: SharedPreferencesHelper = mockk()

  @Test
  fun searchCompositionByIdentifier() = runBlocking {
    coEvery { fhirEngine.search<Composition>(any()) } returns
      listOf(Composition().apply { id = "123" })

    val result = fhirEngine.searchCompositionByIdentifier("appId")

    coVerify { fhirEngine.search<Composition>(any()) }

    Assert.assertEquals("123", result!!.logicalId)
  }

  @Test
  fun loadCqlLibraryBundle() {
    val measureResourceBundleUrl = "measure/ANCIND01-bundle.json"

    val prefsDataKey = SharedPreferenceKey.MEASURE_RESOURCES_LOADED.name
    every { sharedPreferenceHelper.read(prefsDataKey, any<String>()) } returns ""
    every { sharedPreferenceHelper.write(prefsDataKey, any<String>()) } returns Unit
    coEvery { fhirOperator.loadLib(any()) } returns Unit
    coEvery { fhirEngine.create(any()) } returns listOf()

    runBlocking {
      fhirEngine.loadCqlLibraryBundle(
        application,
        sharedPreferenceHelper,
        fhirOperator,
        measureResourceBundleUrl
      )
    }

    Assert.assertNotNull(sharedPreferenceHelper.read(prefsDataKey, ""))
  }
}
