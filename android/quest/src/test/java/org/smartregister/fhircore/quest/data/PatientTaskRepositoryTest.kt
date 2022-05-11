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

package org.smartregister.fhircore.quest.data

import androidx.test.core.app.ApplicationProvider
import com.google.android.fhir.FhirEngine
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.spyk
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Period
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.StringType
import org.hl7.fhir.r4.model.Task
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.util.DateUtils.getDate
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.quest.data.task.PatientTaskRepository
import org.smartregister.fhircore.quest.robolectric.RobolectricTest
import org.smartregister.fhircore.quest.ui.task.PatientTaskItemMapper

@HiltAndroidTest
class PatientTaskRepositoryTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  private lateinit var repository: PatientTaskRepository

  private val fhirEngine: FhirEngine = spyk()

  @Inject lateinit var patientTaskItemMapper: PatientTaskItemMapper

  @Inject lateinit var dispatcherProvider: DispatcherProvider

  @Before
  fun setUp() {
    hiltRule.inject()
    repository =
      PatientTaskRepository(
        context = ApplicationProvider.getApplicationContext(),
        fhirEngine = fhirEngine,
        domainMapper = patientTaskItemMapper,
        dispatcherProvider = dispatcherProvider
      )
  }

  @Test
  fun testLoadAllShouldReturnListOfFamilyItem() {
    val tasks =
      listOf(
        Task().apply {
          id = "1"
          description = "Sick Visit"
          executionPeriod =
            Period().apply {
              start = "2020-03-10".getDate("yyyy-MM-dd")
              end = "2020-03-12".getDate("yyyy-MM-dd")
            }
          `for` = Reference("Patient/2")
        }
      )

    val patient =
      Patient().apply {
        id = "2"
        nameFirstRep.given = listOf(StringType("Name"))
        nameFirstRep.family = "Surname"
        gender = Enumerations.AdministrativeGender.MALE
        addressFirstRep.city = "Nairobi"
      }

    coEvery { fhirEngine.search<Task>(any()) } returns tasks
    coEvery { fhirEngine.get(ResourceType.Patient, any()) } returns patient

    runBlocking {
      val patientTasks = repository.loadData("", 0, true)

      Assert.assertEquals("Name Surname", patientTasks[0].name)
      Assert.assertEquals("1", patientTasks[0].id)
    }
  }

  @Test
  fun testCountAllShouldReturnMoreThanOnePatientCount() {
    coEvery { fhirEngine.count(any()) } returns 5
    val count = runBlocking { repository.countAll() }
    Assert.assertEquals(5, count)
  }
}
