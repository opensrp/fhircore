/*
 * Copyright 2021-2024 Ona Systems, Inc
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

import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Meta
import org.hl7.fhir.r4.model.Patient
import org.junit.Assert
import org.junit.Test
import org.smartregister.fhircore.engine.robolectric.RobolectricTest

class PatientExtensionTest : RobolectricTest() {

  @Test
  fun testExtractFamilyTag() {
    val familyTag =
      Coding().apply {
        system = "http://example.org/family-tags"
        code = "family"
        display = "Family Head"
      }

    val patient = Patient().apply { meta = Meta().apply { tag.add(familyTag) } }

    Assert.assertEquals(
      familyTag,
      patient.extractFamilyTag(),
    )
  }
}
