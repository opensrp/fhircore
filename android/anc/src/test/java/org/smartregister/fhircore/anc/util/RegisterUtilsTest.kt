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

package org.smartregister.fhircore.anc.util

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.hl7.fhir.r4.model.Enumerations
import org.junit.Assert.assertEquals
import org.junit.Test
import org.smartregister.fhircore.anc.robolectric.RobolectricTest

class RegisterUtilsTest : RobolectricTest() {

  @Test
  fun testLoadRegisterConfig() {
    val config =
      ApplicationProvider.getApplicationContext<Application>()
        .loadRegisterConfig(RegisterType.ANC_REGISTER_ID)

    assertEquals(Enumerations.SearchParamType.TOKEN, config.primaryFilter!!.filterType)
    assertEquals(Enumerations.DataType.CODEABLECONCEPT, config.primaryFilter!!.valueType)
    assertEquals("code", config.primaryFilter!!.key)
    assertEquals("LA15173-0", config.primaryFilter!!.valueCoding!!.code)

    assertEquals(Enumerations.SearchParamType.TOKEN, config.secondaryFilter!!.filterType)
    assertEquals(Enumerations.DataType.CODEABLECONCEPT, config.secondaryFilter!!.valueType)
    assertEquals("clinical-status", config.secondaryFilter!!.key)
    assertEquals("active", config.secondaryFilter!!.valueCoding!!.code)
  }
}
