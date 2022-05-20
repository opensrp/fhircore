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

package org.smartregister.fhircore.engine.appfeature

import org.junit.Assert
import org.junit.Test

class AppFeatureTest {

  @Test
  fun testAppFeatures() {
    val appFeatureReport = AppFeature.InAppReporting
    Assert.assertTrue(appFeatureReport.name.equals("InAppReporting", ignoreCase = true))

    val appFeaturePatientManagement = AppFeature.PatientManagement
    Assert.assertTrue(
      appFeaturePatientManagement.name.equals("PatientManagement", ignoreCase = true)
    )

    val appFeatureHouseholdManagement = AppFeature.HouseholdManagement
    Assert.assertTrue(
      appFeatureHouseholdManagement.name.equals("HouseholdManagement", ignoreCase = true)
    )

    val appFeatureDeviceToDeviceSync = AppFeature.DeviceToDeviceSync
    Assert.assertTrue(
      appFeatureDeviceToDeviceSync.name.equals("DeviceToDeviceSync", ignoreCase = true)
    )

    val appFeatureHiv = AppFeature.HivWorkflow
    Assert.assertTrue(appFeatureHiv.name.equals("HivWorkflow", ignoreCase = true))

    val appFeatureTracingAndAppointment = AppFeature.Appointments
    Assert.assertTrue(
      appFeatureTracingAndAppointment.name.equals("Appointments", ignoreCase = true)
    )
  }
}
