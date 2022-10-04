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

package org.smartregister.fhircore.quest.ui.profile

import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.fhir.logicalId
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.mockk
import javax.inject.Inject
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.Patient
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.data.local.register.RegisterRepository
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.util.fhirpath.FhirPathDataExtractor
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.coroutine.CoroutineTestRule
import org.smartregister.fhircore.quest.robolectric.RobolectricTest

@HiltAndroidTest
class ProfileViewModelTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val coroutineRule = CoroutineTestRule()

  @Inject lateinit var registerRepository: RegisterRepository

  private val configurationRegistry: ConfigurationRegistry = Faker.buildTestConfigurationRegistry()

  @Inject lateinit var fhirPathDataExtractor: FhirPathDataExtractor

  private lateinit var profileViewModel: ProfileViewModel

  private lateinit var resourceData: ResourceData

  private lateinit var expectedBaseResource: Patient

  @Before
  fun setUp() {
    hiltRule.inject()
    expectedBaseResource = Faker.buildPatient()
    resourceData = ResourceData(baseResource = expectedBaseResource)
    registerRepository = mockk()
    coEvery { registerRepository.loadProfileData(any(), any()) } returns resourceData

    runBlocking {
      configurationRegistry.loadConfigurations(
        context = InstrumentationRegistry.getInstrumentation().targetContext,
        appId = APP_DEBUG
      ) {}
    }

    profileViewModel =
      ProfileViewModel(
        registerRepository = registerRepository,
        configurationRegistry = configurationRegistry,
        dispatcherProvider = coroutineRule.testDispatcherProvider,
        fhirPathDataExtractor = fhirPathDataExtractor
      )
  }

  @Test
  fun testRetrieveProfileUiState() {
    runBlocking { profileViewModel.retrieveProfileUiState("householdProfile", "sampleId") }

    assertNotNull(profileViewModel.profileUiState.value)
    val actualPatient = profileViewModel.profileUiState.value.resourceData?.baseResource as Patient
    assertNotNull(actualPatient)
    assertEquals(expectedBaseResource.logicalId, actualPatient.logicalId)
    assertEquals(expectedBaseResource.name[0].family, actualPatient.name[0].family)
    assertEquals(expectedBaseResource.name[0].given, actualPatient.name[0].given)
    assertEquals(expectedBaseResource.address[0].city, actualPatient.address[0].city)

    val profileConfiguration = profileViewModel.profileUiState.value.profileConfiguration
    assertEquals("app", profileConfiguration?.appId)
    assertEquals("profile", profileConfiguration?.configType)
    assertEquals("householdProfile", profileConfiguration?.id)
  }
}
