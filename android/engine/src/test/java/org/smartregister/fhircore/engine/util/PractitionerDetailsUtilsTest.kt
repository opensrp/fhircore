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

package org.smartregister.fhircore.engine.util

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.android.fhir.FhirEngine
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.spyk
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.hl7.fhir.r4.model.CareTeam
import org.hl7.fhir.r4.model.Location
import org.hl7.fhir.r4.model.Organization
import org.hl7.fhir.r4.model.Parameters
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.StringType
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.robolectric.RobolectricTest

@ExperimentalCoroutinesApi
@HiltAndroidTest
class PractitionerDetailsUtilsTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val instantTaskExecutorRule = InstantTaskExecutorRule()

  private lateinit var practitionerDetailsUtils: PractitionerDetailsUtils

  @Inject lateinit var sharedPreferencesHelper: SharedPreferencesHelper

  private val careTeamList: List<CareTeam> = getCareTeams()

  private val organizationList: List<Organization> = getOrganizations()

  private val locationList: List<Location> = getLocations()

  @Before
  fun setUp() {
    hiltRule.inject()
    // Spy needed to control interaction with the real injected dependency
    val fhirEngine = spyk<FhirEngine>()

    practitionerDetailsUtils =
      PractitionerDetailsUtils(
        fhirEngine = fhirEngine,
        sharedPreferences = sharedPreferencesHelper,
      )
  }

  @Test
  fun addParametersShouldAddParameters() {
    val parameters = Parameters()
    parameters.addParameter().apply {
      name = ResourceType.Practitioner.name
      value = StringType("123")
    }
    Assert.assertEquals(parameters.parameter.size, 1)
    practitionerDetailsUtils.addParameters(
      careTeamList,
      parameters = parameters,
      ResourceType.CareTeam.name
    )
    Assert.assertEquals(parameters.parameter.size, 2)
    practitionerDetailsUtils.addParameters(
      organizationList,
      parameters = parameters,
      ResourceType.Organization.name
    )
    Assert.assertEquals(parameters.parameter.size, 3)
    practitionerDetailsUtils.addParameters(
      locationList,
      parameters = parameters,
      ResourceType.Location.name
    )
    Assert.assertEquals(parameters.parameter.size, 4)
  }

  private fun getCareTeams(): List<CareTeam> {
    val listOfCareTeams = arrayListOf<CareTeam>()
    listOfCareTeams.add(
      CareTeam().apply {
        this.name = "abc"
        this.id = "204"
      }
    )
    return listOfCareTeams
  }

  private fun getOrganizations(): List<Organization> {
    val listOfOrganization = arrayListOf<Organization>()
    listOfOrganization.add(
      Organization().apply {
        this.name = "abc"
        this.id = "24"
      }
    )
    return listOfOrganization
  }

  private fun getLocations(): List<Location> {
    val listOfLocation = arrayListOf<Location>()
    listOfLocation.add(
      Location().apply {
        this.name = "abc"
        this.id = "20"
      }
    )
    return listOfLocation
  }
}
