/*
 * Copyright 2021-2023 Ona Systems, Inc
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

import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Observation
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class ObservationExtensionTest {
  private lateinit var observation: Observation

  @Before
  fun setup() {
    observation = Observation()
  }

  @Test
  fun testCodingOf() {
    val code = "code"
    val coding = Coding("test", code, code)
    observation.code.coding = listOf(coding)
    Assert.assertEquals(coding, observation.codingOf(code))
  }

  @Test
  fun testValueCodeDoesNotExist() {
    Assert.assertEquals(null, observation.valueCode())
  }

  @Test
  fun testValueCodeExists() {
    val code = "code"
    val coding = Coding("test", code, code)
    observation.value = CodeableConcept(coding)
    Assert.assertEquals(code, observation.valueCode())
  }
}
