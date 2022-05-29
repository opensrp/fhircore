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

package org.smartregister.fhircore.engine.domain.model

import org.junit.Assert
import org.junit.Test

class PatientTypeTest {

  @Test
  fun testPatientTypes() {
    Assert.assertNotNull(PatientType.valueOf("NEWLY_DIAGNOSED_CLIENT"))
    Assert.assertNotNull(PatientType.valueOf("CLIENT_ALREADY_ON_ART"))
    Assert.assertNotNull(PatientType.valueOf("EXPOSED_INFANT"))
    Assert.assertNotNull(PatientType.valueOf("COMMUNITY_POSITIVE"))
    Assert.assertNotNull(PatientType.valueOf("CHILD_CONTACT"))
    Assert.assertNotNull(PatientType.valueOf("SEXUAL_CONTACT"))
  }
}
