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

package org.smartregister.fhircore.engine.data.local.regitser.dao

import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.smartregister.fhircore.engine.appfeature.model.HealthModule
import org.smartregister.fhircore.engine.data.local.register.dao.AncPatientRegisterDao
import org.smartregister.fhircore.engine.data.local.register.dao.AppointmentRegisterDao
import org.smartregister.fhircore.engine.data.local.register.dao.DefaultPatientRegisterDao
import org.smartregister.fhircore.engine.data.local.register.dao.FamilyRegisterDao
import org.smartregister.fhircore.engine.data.local.register.dao.HivRegisterDao
import org.smartregister.fhircore.engine.data.local.register.dao.RegisterDaoFactory
import org.smartregister.fhircore.engine.robolectric.RobolectricTest

@HiltAndroidTest
class RegisterDaoFactoryTest : RobolectricTest() {

  @Test
  fun testVerifyRegisterDaoMap() {

    val ancPatientRegisterDao = mockk<AncPatientRegisterDao>()
    val defaultPatientRegisterDao = mockk<DefaultPatientRegisterDao>()
    val familyRegisterDao = mockk<FamilyRegisterDao>()
    val hivRegisterDao = mockk<HivRegisterDao>()
    val appointmentRegisterDao = mockk<AppointmentRegisterDao>()

    val registerDaoFactory =
      RegisterDaoFactory(
        ancPatientRegisterDao,
        defaultPatientRegisterDao,
        familyRegisterDao,
        hivRegisterDao,
        appointmentRegisterDao
      )

    with(registerDaoFactory.registerDaoMap) {
      assertEquals(6, size)
      assertNotNull(get(HealthModule.ANC))
      assertNotNull(get(HealthModule.FAMILY))
      assertNotNull(get(HealthModule.HIV))
      assertNotNull(get(HealthModule.TRACING))
      assertNotNull(get(HealthModule.APPOINTMENT))
      assertNotNull(get(HealthModule.DEFAULT))
    }
  }
}
