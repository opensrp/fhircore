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

package org.smartregister.fhircore.anc.ui.family.removememberfamily

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.android.fhir.FhirEngine
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.hl7.fhir.r4.model.HumanName
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.StringType
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.anc.coroutine.CoroutineTestRule
import org.smartregister.fhircore.anc.data.family.FamilyDetailRepository
import org.smartregister.fhircore.anc.data.patient.DeletionReason
import org.smartregister.fhircore.anc.data.patient.PatientRepository
import org.smartregister.fhircore.anc.robolectric.RobolectricTest
import org.smartregister.fhircore.anc.ui.family.removefamilymember.RemoveFamilyMemberQuestionnaireViewModel
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.cql.LibraryEvaluator
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper

@ExperimentalCoroutinesApi
@HiltAndroidTest
class RemoveFamilyMemberReasonMovedAwayQuestionnaireViewModelTest : RobolectricTest() {

  @BindValue val sharedPreferencesHelper: SharedPreferencesHelper = mockk(relaxed = true)

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) var instantTaskExecutorRule = InstantTaskExecutorRule()

  @get:Rule(order = 2) var coroutineRule = CoroutineTestRule()

  private val fhirEngine: FhirEngine = mockk()

  private lateinit var viewModel: RemoveFamilyMemberQuestionnaireViewModel

  private lateinit var defaultRepo: DefaultRepository
  private var patientRepository: PatientRepository = mockk(relaxed = true)
  private var familyDetailRepository: FamilyDetailRepository = mockk(relaxed = true)
  private val libraryEvaluator: LibraryEvaluator = mockk()

  @Before
  fun setUp() {
    hiltRule.inject()

    defaultRepo = spyk(DefaultRepository(fhirEngine, DefaultDispatcherProvider()))
    val configurationRegistry = mockk<ConfigurationRegistry>()
    every { configurationRegistry.appId } returns "appId"
    viewModel =
      spyk(
        RemoveFamilyMemberQuestionnaireViewModel(
          fhirEngine = fhirEngine,
          defaultRepository = defaultRepo,
          configurationRegistry = configurationRegistry,
          transformSupportServices = mockk(),
          patientRepository = patientRepository,
          familyDetailRepository = familyDetailRepository,
          dispatcherProvider = defaultRepo.dispatcherProvider,
          sharedPreferencesHelper = sharedPreferencesHelper,
          libraryEvaluator = libraryEvaluator
        )
      )

    ReflectionHelpers.setField(viewModel, "defaultRepository", defaultRepo)
    coEvery { viewModel.reasonRemove } returns "Moved away"
    coEvery { familyDetailRepository.fetchDemographics("111") } returns getPatient()
  }

  private fun getPatient(): Patient {
    val patient =
      Patient().apply {
        id = "111"
        name =
          listOf(
            HumanName().apply {
              given = listOf(StringType("john"))
              family = "doe"
            }
          )
      }
    return patient
  }

  @Test
  fun testGetReasonRemoveOther() {
    val deletionReasonTwo = viewModel.getReasonRemove()
    Assert.assertEquals(deletionReasonTwo, DeletionReason.MOVED_AWAY)
  }
}
