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

package org.smartregister.fhircore.quest.data.patient

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.Patient
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.domain.repository.PatientDao
import org.smartregister.fhircore.quest.coroutine.CoroutineTestRule

@OptIn(ExperimentalCoroutinesApi::class)
class HivPatientGuardianRepositoryTest {
  @get:Rule val coroutineTestRule = CoroutineTestRule()

  private val patientDao: PatientDao = mockk()
  private lateinit var hivPatientGuardianRepository: HivPatientGuardianRepository

  @Before
  fun setUp() {
    hivPatientGuardianRepository =
      HivPatientGuardianRepository(patientDao, coroutineTestRule.testDispatcherProvider)
  }

  @Test
  fun loadPatient() = runTest {
    coEvery { patientDao.loadPatient(any()) } returns null
    val unknownId = "-1"
    val result = hivPatientGuardianRepository.loadPatient(unknownId)
    Assert.assertNull(result)
    coVerify { patientDao.loadPatient(unknownId) }
  }

  @Test
  fun loadGuardianRegisterData() = runTest {
    val patient = Patient().apply { id = "test-id" }
    coEvery { patientDao.loadGuardiansRegisterData(patient) } returns emptyList()
    val result = hivPatientGuardianRepository.loadGuardianRegisterData(patient)
    Assert.assertEquals(0, result.size)
    coVerify { patientDao.loadGuardiansRegisterData(patient) }
  }
}
