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

package org.smartregister.fhircore.engine.appfeature.model

import org.junit.Assert
import org.junit.Test

class HealthModuleTest {

  @Test
  fun testAppFeatures() {
    Assert.assertNotNull(HealthModule.valueOf("ANC"))
    Assert.assertNotNull(HealthModule.valueOf("RDT"))
    Assert.assertNotNull(HealthModule.valueOf("PNC"))
    Assert.assertNotNull(HealthModule.valueOf("FAMILY"))
    Assert.assertNotNull(HealthModule.valueOf("CHILD"))
    Assert.assertNotNull(HealthModule.valueOf("FAMILY_PLANNING"))
    Assert.assertNotNull(HealthModule.valueOf("HIV"))
    Assert.assertNotNull(HealthModule.valueOf("TRACING"))
    Assert.assertNotNull(HealthModule.valueOf("APPOINTMENT"))
  }
}
